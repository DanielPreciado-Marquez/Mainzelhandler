package de.wwu.imi.pseudonymizer.lib.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.wwu.imi.pseudonymizer.lib.entities.Patient;
import de.wwu.imi.pseudonymizer.lib.repositories.PatientRepository;

/**
 * Controller that can talk to the Mainzelliste service. Its main purpose is to
 * get a URL from the Mainzelliste that contains session and query tokens and is
 * ready to be used to pseudonymize a patient.
 *
 * That way the pii (personally identifiable information) doesn't have to be
 * sent to this controller and instead the client can do the pseudonymization
 * themselves by sending the pii to the mainzelliste URL mentioned above.
 *
 * @see <a href=
 *      "https://bitbucket.org/medicalinformatics/mainzelliste/src/master/">Mainzelliste
 *      Source Code</a>
 */
@RestController
@CrossOrigin
public class PseudonymizationController {

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PseudonymizationController.class);

	// mainzelliste configurations are loaded from application.properties
	@Value("${mainzelliste.url}")
	private String mainzellisteUrl;

	@Value("${mainzelliste.apikey}")
	private String mainzellisteApiKey;

	@Autowired
	private PatientRepository patientRepository;

	/**
	 * Handles the tokening with the pseudonymization server and returns an Array of
	 * urls with the appropriate token. Allowed token types are "addPatient",
	 * "readPatient".
	 * 
	 * @param type   Type of the token.
	 * @param amount Amount of tokens.
	 * @return An array of urls with the appropriate token.
	 */
	@GetMapping("/api/tokens/{type}/{amount}")
	public String[] getPseudonymizationURL(@PathVariable("type") final String type,
			@PathVariable("amount") final String amount) {

		final var amountParsed = Integer.parseInt(amount);
		LOGGER.info("Requesting " + amount + " \"" + type + "\" tokens");

		HttpClient httpClient = HttpClientBuilder.create().build();
		String sessionURL = getSessionURL(httpClient);
		LOGGER.debug("Session url: " + sessionURL);

		var urlTokens = new String[amountParsed];
		for (int i = 0; i < amountParsed; i++) {
			urlTokens[i] = mainzellisteUrl + "patients?tokenId=" + getTokenId(sessionURL, type, httpClient);
		}

		LOGGER.debug("Request resolved");
		return urlTokens;
	}

	@PostMapping("/api/patients/save")
	public void addPatient(@RequestBody final List<Patient> patients) {
		for (final var patient : patients) {
			// TODO proper error handling
			LOGGER.debug("Recieved Patient: " + patient.toString());
			patientRepository.save(patient);
		}
	}

	@PostMapping("/api/patients/load")
	public List<String> getMdat(@RequestBody final List<String> pseudonyms) {
		List<String> mdat = new ArrayList<>();

		// TODO It may be more efficient to use findAllById
		// This would require some assigning of the returned mdat
		for (final String pseudonym : pseudonyms) {
			LOGGER.debug("Searching: " + pseudonym);
			final var patient = patientRepository.findById(pseudonym);
			
			if (patient.isPresent()) {
				mdat.add(patient.get().getMdat());
			} else {
				// TODO proper handling
				mdat.add("");
			}
		}
		return mdat;
	}

	/**
	 * Gets a session url with a valid session token from the pseudonymization
	 * server with configured baseURL.
	 *
	 * @param httpClient A HTTP-Client for the connection.
	 * @return The session url with a valid session token from the pseudonymization
	 *         server.
	 */
	private String getSessionURL(final HttpClient httpClient) {
		String connectionUrl = mainzellisteUrl + "sessions/";
		HttpPost request = new HttpPost(connectionUrl);
		request.addHeader("mainzellisteApiKey", mainzellisteApiKey);
		try {
			HttpResponse httpResponse = httpClient.execute(request);
			InputStream connectionResponse = httpResponse.getEntity().getContent();
			String response = IOUtils.toString(connectionResponse, StandardCharsets.UTF_8);
			JSONObject jsonResponse = new JSONObject(response);
			return jsonResponse.getString("uri");

		} catch (IOException exception) {
			LOGGER.debug("Error while connecting to the pseudonymization server: {}", exception.getLocalizedMessage());
		}
		return "";
	}

	/**
	 * Gets a query token which allows to add a new patient from the
	 * pseudonymization server with given sessionURL.
	 *
	 * @param sessionUrl A session url with a valid session token from the
	 *                   pseudonymization server.
	 * @param httpClient A HTTP-Client for the connection.
	 * @return The query token from the pseudonymization server.
	 */
	private String getTokenId(final String sessionUrl, final String tokenType, final HttpClient httpClient) {
		String connectionUrl = sessionUrl + "tokens/";
		HttpPost request = new HttpPost(connectionUrl);
		request.addHeader("content-type", "application/json");
		request.addHeader("mainzellisteApiKey", mainzellisteApiKey);
		JSONObject type = new JSONObject();
		JSONObject callback = new JSONObject();
		type.put("data", callback);
		type.put("type", tokenType);
		request.setEntity(new StringEntity(type.toString(), ContentType.APPLICATION_JSON));
		try {
			HttpResponse httpResponse = httpClient.execute(request);
			InputStream connectionResponse = httpResponse.getEntity().getContent();
			String response = IOUtils.toString(connectionResponse, StandardCharsets.UTF_8);
			JSONObject jsonResponse = new JSONObject(response);
			return jsonResponse.getString("tokenId");
		} catch (IOException exception) {
			LOGGER.debug("Error while connecting to the pseudonymization server: {}", exception.getLocalizedMessage());
		}
		return "";
	}

}
