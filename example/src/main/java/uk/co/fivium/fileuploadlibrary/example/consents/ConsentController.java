package uk.co.fivium.fileuploadlibrary.example.consents;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@Controller
@RequestMapping("consents")
public class ConsentController {

  private final ConsentService consentService;

  ConsentController(ConsentService consentService) {
    this.consentService = consentService;
  }

  @GetMapping
  ModelAndView getConsents() {
    return new ModelAndView("consents/index")
        .addObject("consentList", consentService.getAllConsentDetails());
  }

  @PostMapping
  ModelAndView createConsent() {
    var url = MvcUriComponentsBuilder.fromMethodName(this.getClass(), "getConsents").toUriString();
    return new ModelAndView("redirect:%s".formatted(url));
  }

}
