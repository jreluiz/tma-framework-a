package eubrazil.atmosphere.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.qualitymodel.entity.ConfigurationProfile;

@Repository
public interface ConfigurationProfileRepository extends CrudRepository<ConfigurationProfile, Long> {

	@Override
	List<ConfigurationProfile> findAll();

	@Query(value="select cp from ConfigurationProfile cp join fetch cp.metrics join fetch "
			+ "cp.preferences where cp.configurationprofileId = ?1")
    List<ConfigurationProfile> findConfigurationProfileInstance(Integer configurationProfileID);
	
}
