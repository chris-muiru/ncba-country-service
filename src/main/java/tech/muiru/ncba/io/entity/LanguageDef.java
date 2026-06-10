package tech.muiru.ncba.io.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.muiru.ncba.util.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "language")
@Getter
@Setter
@NoArgsConstructor
public class LanguageDef extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "language_seq")
    @SequenceGenerator(name = "language_seq", sequenceName = "language_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID languageUUID;

    private String isoCode;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_country_id")
    private CountryInfoDef countryInfo;

    @PrePersist
    private void prePersist() {
        if (languageUUID == null) {
            languageUUID = UUID.randomUUID();
        }
    }
}
