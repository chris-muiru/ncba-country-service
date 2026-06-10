package tech.muiru.ncba.country.service;

import tech.muiru.ncba.dto.CountryInfoDTO;

import java.util.List;

public interface CountryService {

    CountryInfoDTO fetchAndSave(String countryName);

    List<CountryInfoDTO> getAll();

    CountryInfoDTO getById(Long id);

    CountryInfoDTO update(Long id, CountryInfoDTO dto);

    void delete(Long id);
}
