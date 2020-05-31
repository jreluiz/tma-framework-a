package eubrazil.atmosphere.job;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

import eubr.atmosphere.tma.entity.qualitymodel.CompositeAttribute;
import eubr.atmosphere.tma.entity.qualitymodel.ConfigurationProfile;
import eubr.atmosphere.tma.entity.qualitymodel.HistoricalData;
import eubr.atmosphere.tma.exceptions.UndefinedException;
import eubr.atmosphere.tma.utils.ListUtils;
import eubr.atmosphere.tma.utils.MessagePlanning;
import eubr.atmosphere.tma.utils.TreeUtils;
import eubrazil.atmosphere.config.quartz.SchedulerConfig;
import eubrazil.atmosphere.kafka.KafkaManager;
import eubrazil.atmosphere.qualitymodel.SpringContextBridge;
import eubrazil.atmosphere.service.TrustworthinessService;

/**
 * Trustworthiness Poll Job
 * @author JorgeLuiz
 */
@Component
@DisallowConcurrentExecution
@PropertySource(ignoreResourceNotFound = true, value = "classpath:config.properties")
public class TrustworthinessAnalyzePollJob implements Job {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private static final Integer TRUSTWORTHINESS_CONFIGURATION_PROFILE_ID = 1;
	
	@Value("${trigger.job.time}")
	private String triggerJobTime;

	@Override
	public void execute(JobExecutionContext jobExecutionContext) {
		LOGGER.info("TrustworthinessAnalyzePollJob - execution..");

		TrustworthinessService trustworthinessService = SpringContextBridge.services().getTrustworthinessService();
		List<ConfigurationProfile> configProfileList = trustworthinessService.findConfigurationProfileInstance(TRUSTWORTHINESS_CONFIGURATION_PROFILE_ID);

		if ( ListUtils.isEmpty(configProfileList) ) {
			LOGGER.error("Quality Model for trustworthiness not defined in the database.");
			return;
		}

		ConfigurationProfile configurationActor =  ListUtils.getFirstElement(configProfileList);
		LOGGER.info("TrustworthinessAnalyzePollJob (TrustworthinessPollJob) - ConfigurationProfile: " + configurationActor);
		
		boolean isNewDataInsertedForMetrics = trustworthinessService.isNewDataInsertedForMetrics(configurationActor.getMetrics());
		if ( !isNewDataInsertedForMetrics ) {
			LOGGER.info(new Date() + " - No new data entered for metrics in the Data table.");
			return;
		}
		
		LOGGER.info("New data entered for metrics in the Data table.");
		CompositeAttribute privacy = TreeUtils.getInstance().getRootAttribute(configurationActor);

		try {
			HistoricalData historicalData = null;
			historicalData = privacy.calculate(configurationActor);
			LOGGER.info(new Date() + " - Calculated score for trustworthiness: " + historicalData.getValue());

			MessagePlanning messagePlanning = new MessagePlanning(
					ListUtils.getFirstElement(configProfileList).getConfigurationprofileId());
			
			// Add calculated score to kafka topic
			KafkaManager.getInstance().addItemKafka(messagePlanning);
		} catch (UndefinedException e) {
			LOGGER.error("Property not defined in the quality model ", e);
		} catch (InterruptedException e) {
			LOGGER.error("InterruptedException when adding kafka item: ", e);
		} catch (ExecutionException e) {
			LOGGER.error("ExecutionException when adding kafka item: ", e);
		}

		LOGGER.info("TrustworthinessAnalyzePollJob - end of execution..");
	}
	
	@Bean(name = "jobBean1")
	public JobDetailFactoryBean job() {
		return SchedulerConfig.createJobDetail(this.getClass());
	}

	@Bean(name = "jobBean1Trigger")
	public CronTriggerFactoryBean jobTrigger(@Qualifier("jobBean1") JobDetail jobDetail) {
		return SchedulerConfig.createCronTrigger(jobDetail, "*/20 * * * * ?");
	}
	
}
