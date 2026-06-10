package tech.muiru.ncba.io.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.muiru.ncba.util.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "country_info")
@Getter
@Setter
@NoArgsConstructor
public class CountryInfoDef extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "country_info_seq")
    @SequenceGenerator(name = "country_info_seq", sequenceName = "country_info_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID countryUUID;
    private String name;
    private String capitalCity;
    private String phoneCode;
    private String continentCode;
    private String currencyIsoCode;
    private String countryFlag;
    private String isoCode;

    @OneToMany(mappedBy = "countryInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LanguageDef> languages = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (countryUUID == null) {
            countryUUID = UUID.randomUUID();
        }
    }
}
