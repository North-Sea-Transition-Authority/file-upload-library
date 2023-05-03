package uk.co.fivium.fileuploadlibrary.example.consents;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConsentRepository extends JpaRepository<ConsentDetail, Long> {

}
