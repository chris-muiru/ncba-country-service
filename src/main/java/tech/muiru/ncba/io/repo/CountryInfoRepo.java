package tech.muiru.ncba.io.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.muiru.ncba.io.entity.CountryInfoDef;

import java.util.Optional;
import java.util.UUID;

public interface CountryInfoRepo extends JpaRepository<CountryInfoDef, Long> {

    Optional<CountryInfoDef> findByCountryUUID(UUID uuid);
}
