package uk.co.fivium.fileuploadlibrary.example.consents;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ConsentService {

  private final ConsentRepository consentRepository;

  ConsentService(ConsentRepository consentRepository) {
    this.consentRepository = consentRepository;
  }

  List<ConsentDetail> getAllConsentDetails() {
    return consentRepository.findAll();
  }

}
