package eubrazil.atmosphere.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.Data;
import eubr.atmosphere.tma.entity.qualitymodel.HistoricalData;
import eubr.atmosphere.tma.entity.qualitymodel.LeafAttribute;
import eubr.atmosphere.tma.entity.qualitymodel.Metric;
import eubr.atmosphere.tma.utils.ListUtils;
import eubrazil.atmosphere.config.appconfig.PropertiesManager;
import eubrazil.atmosphere.repository.ConfigurationProfileRepository;
import eubrazil.atmosphere.repository.DataRepository;
import eubrazil.atmosphere.repository.HistoricalDataRepository;
import eubrazil.atmosphere.repository.MetricRepository;
import eubrazil.atmosphere.service.TrustworthinessService;

/**
 * Implementation of trustworthiness services
 * @author JorgeLuiz
 */
@Service
public class TrustworthinessServiceImpl implements TrustworthinessService {

	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private HistoricalDataRepository historicalDataRepository;
	
	@Autowired
	private ConfigurationProfileRepository configurationProfileRepository;
	
	@Autowired
	private MetricRepository metricRepository;
	
	@Override
	public List<Data> getLimitedDataListById(Integer probeId, Integer descriptionId, Integer resourceId,
			Pageable numSamples) {
		return dataRepository.getLimitedDataListById(probeId, descriptionId, resourceId, numSamples);
	}
	
	@Override
	public List<Data> getLimitedDataListByIdAndTimestamp(Integer probeId, Integer descriptionId, Integer resourceId, Date timestamp) {
		return dataRepository.getLimitedDataListByIdAndTimestamp(probeId, descriptionId, resourceId, timestamp);
	}
	
	@Override
	public void save(HistoricalData historicalData) {
		historicalDataRepository.save(historicalData);
	}
	
	@Override
	public List<ConfigurationProfile> findConfigurationProfileInstance(Integer configurationProfileID) {
		return configurationProfileRepository.findConfigurationProfileInstance(configurationProfileID);
	}
	
	@Override
	public Date getLastTimestampInsertedForMetrics(Set<Metric> metrics) {
		
		Date lastTime = null;
		
		Integer probeId = Integer.parseInt(PropertiesManager.getInstance().getProperty("probe.id"));
		Integer resourceId = Integer.parseInt(PropertiesManager.getInstance().getProperty("resource.id"));
		Integer descriptionId = null;
		
		for (Metric metric : metrics) {
			if (metric.getDescriptionName().equalsIgnoreCase("InformationLossMetric")) {
				descriptionId = Integer.parseInt(PropertiesManager.getInstance().getProperty("score")); // loss
			} else {
				descriptionId = Integer.parseInt(PropertiesManager.getInstance().getProperty("riskP")); // risk
			}
			List<Data> lData = dataRepository.getLimitedDataListById(probeId, descriptionId, resourceId, new PageRequest (0, 1));
			Data lastDataInserted = Collections.max(lData, Comparator.comparing(d -> d.getId().getValueTime()));
			if (lastDataInserted != null && (lastTime == null
					|| (lastTime != null && lastTime.before(lastDataInserted.getId().getValueTime())))) {
				lastTime = lastDataInserted.getId().getValueTime();
			}
		}
		
		return lastTime;
	}
	
	@Override
	public boolean isNewDataInsertedForMetrics(Set<Metric> metrics) {

		boolean newDataInserted = false; 
		
		for (Metric metric : metrics) {
			
			// get last data inserted for metric
			LeafAttribute metricLeafAttr = metric.getAttribute();
			List<Data> lData = dataRepository.getLimitedDataListById(metric.getProbeId(), metric.getDescriptionId(),
					metric.getResourceId(), metricLeafAttr != null ? new PageRequest(0, metricLeafAttr.getNumSamples())
							: new PageRequest(0, 1));
			Data currentDataInsertedForMetric = null;
			if ( ListUtils.isNotEmpty(lData) ) {
				currentDataInsertedForMetric = Collections.max(lData, Comparator.comparing(d -> d.getId().getValueTime()));
			}
			
			// get current timestamp inserted for metric (referent to current data)
			Date currentTimestampInsertedForMetric = null;
			if (currentDataInsertedForMetric != null && currentDataInsertedForMetric.getId().getValueTime() != null) {
				currentTimestampInsertedForMetric = currentDataInsertedForMetric.getId().getValueTime();
			} else {
				continue;
			}
			
			// update last timestamp inserted for metric
			Date lastTimestampValueForMetric = metric.getLastTimestampDataInserted();
			if ((lastTimestampValueForMetric == null && currentDataInsertedForMetric != null)
					|| (lastTimestampValueForMetric != null
							&& lastTimestampValueForMetric.before(currentTimestampInsertedForMetric))) {
				metric.setLastTimestampDataInserted(currentTimestampInsertedForMetric);
				metricRepository.save(metric);
				newDataInserted = true;
			}
			
		}
		
		return newDataInserted;
	}
	
	@Override
	public Double getInstanceValueById() {
		
		Double instanceValue = null;
		
		Integer probeId = Integer.parseInt(PropertiesManager.getInstance().getProperty("probe.id"));
		Integer resourceId = Integer.parseInt(PropertiesManager.getInstance().getProperty("resource.id"));
		Integer descriptionId = Integer.parseInt(PropertiesManager.getInstance().getProperty("id"));
		
		List<Double> instanceValues = dataRepository.getInstanceValueById(probeId, descriptionId, resourceId);
		if ( ListUtils.isNotEmpty(instanceValues) ) {
			instanceValue = ListUtils.getFirstElement(instanceValues);
		}
		
		return instanceValue;
	}

}
