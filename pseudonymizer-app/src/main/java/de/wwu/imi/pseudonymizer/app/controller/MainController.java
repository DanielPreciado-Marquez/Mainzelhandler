package de.wwu.imi.pseudonymizer.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main Controller that renders our *beautiful* UI.
 */
@Controller
@CrossOrigin
public class MainController {
	
    @GetMapping("/")
    public String welcome(Model model) {
        return "welcome"; // welcome.html in src/main/resources/templates/
    }
    
    @GetMapping("/pseudonymization")
    public String pseudonymizaton(Model model) {
        return "pseudonymization";
    }
    
    @GetMapping("/depseudonymization")
    public String depseudonymizaton(Model model) {
        return "depseudonymization";
    }
}