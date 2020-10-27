package de.wwu.imi.demonstrator.app.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wwu.imi.demonstrator.app.entities.PatientEntity;
import de.wwu.imi.demonstrator.app.repositories.PatientRepository;
import de.wwu.imi.pseudonymizer.lib.controller.AbstractPseudonymizationController;
import de.wwu.imi.pseudonymizer.lib.model.Patient;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class PatientController extends AbstractPseudonymizationController {

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PatientController.class);

	@Autowired
	private PatientRepository patientRepository;

	@Override
	public void acceptPatients(List<Patient> patients) {
		LOGGER.debug("Storing " + patients.size() + " patients");

		for (final var patient : patients) {
			LOGGER.trace("Storing Patient: " + patient.toString());

			final var patientEntity = new PatientEntity(patient);

			patientRepository.save(patientEntity);
		}
		LOGGER.debug("Storing completed");
	}

	@Override
	public List<Patient> requestPatients(List<String> pseudonyms) {
		LOGGER.debug("Requesting " + pseudonyms.size() + " patients");

		final var patients = new ArrayList<Patient>();

		for (final String pseudonym : pseudonyms) {
			LOGGER.trace("Searching: " + pseudonym);

			final var patientEntity = patientRepository.findById(pseudonym);

			if (patientEntity.isPresent()) {
				patients.add(new Patient(pseudonym, patientEntity.get().getMdat()));
			} else {
				patients.add(new Patient(pseudonym, "{}"));
			}
		}
		LOGGER.debug("Request resolved");

		return patients;
	}

}