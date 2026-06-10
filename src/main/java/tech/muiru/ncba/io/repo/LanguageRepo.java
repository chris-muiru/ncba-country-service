package tech.muiru.ncba.io.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.muiru.ncba.io.entity.LanguageDef;

public interface LanguageRepo extends JpaRepository<LanguageDef, Long> {
}
