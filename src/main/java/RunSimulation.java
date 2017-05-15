import com.pb.common.matrix.Matrix;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by carlloga on 15-05-17.
 */
public class RunSimulation {

    String inputNetworkFile;
    String scheduleFile;
    String vehicleFile;

    String runId;
    String outputDirectory;
    String plansFile;


    public RunSimulation() {
        inputNetworkFile = "./input/networkMunich.xml.gz";
        //scheduleFile = "schedule.xml";
        //vehicleFile = "vehicles.xml";

        runId = "Test";
        outputDirectory = "output";
        plansFile = "./input/plans.xml.gz";

    }

    public void runMatsim() {

        final Config config = ConfigUtils.createConfig();

        // Global
        config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        // Network
        config.network().setInputFile(inputNetworkFile);

        //public transport
        //config.transit().setTransitScheduleFile(scheduleFile);
       // config.transit().setVehiclesFile(vehicleFile);
        config.transit().setUseTransit(false);
        //Set<String> transitModes = new TreeSet<>();
        //transitModes.add("pt");
        //config.transit().setTransitModes(transitModes);


        // Plans
        //		config.plans().setInputFile(inputPlansFile);

        // Simulation
        //		config.qsim().setFlowCapFactor(0.01);
        config.qsim().setFlowCapFactor(1);
        //		config.qsim().setStorageCapFactor(0.018);
        config.qsim().setStorageCapFactor(1);
        config.qsim().setRemoveStuckVehicles(false);

        config.qsim().setStartTime(0);
        config.qsim().setEndTime(24*60*60);

        // Controller
        //		String siloRunId = "run_09";

        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setFirstIteration(1);

        int numberOfIterations = 10;

        config.controler().setLastIteration(numberOfIterations);
        config.controler().setMobsim("qsim");

        config.controler().setWritePlansInterval(numberOfIterations);
        config.controler().setWriteEventsInterval(numberOfIterations);
        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        //linkstats
//        config.linkStats().setWriteLinkStatsInterval(1);
//        config.linkStats().setAverageLinkStatsOverIterations(0);

        // QSim and other
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        //Strategy
        StrategyConfigGroup.StrategySettings strategySettings1 = new StrategyConfigGroup.StrategySettings();
        strategySettings1.setStrategyName("ChangeExpBeta");
        strategySettings1.setWeight(0.5); //originally 0.8
        config.strategy().addStrategySettings(strategySettings1);

        StrategyConfigGroup.StrategySettings strategySettings2 = new StrategyConfigGroup.StrategySettings();
        strategySettings2.setStrategyName("ReRoute");
        strategySettings2.setWeight(1);//originally 0.2
        strategySettings2.setDisableAfter((int) (numberOfIterations * 0.7));
        config.strategy().addStrategySettings(strategySettings2);

        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(1); //originally 0
        strategySettings3.setDisableAfter((int) (numberOfIterations * 0.7));
        config.strategy().addStrategySettings(strategySettings3);

        //TODO this strategy is implemented to test the pt modes (in general do not include)
//        StrategyConfigGroup.StrategySettings strategySettings4 = new StrategyConfigGroup.StrategySettings();
//        strategySettings4.setStrategyName("ChangeTripMode");
//        strategySettings4.setWeight(0); //originally 0
//        strategySettings4.setDisableAfter((int) (numberOfIterations * 0.7));
//        config.strategy().addStrategySettings(strategySettings4);


        config.strategy().setMaxAgentPlanMemorySize(4);

        // Plan Scoring (planCalcScore)
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(workActivity);

        PlanCalcScoreConfigGroup.ActivityParams otherActivity = new PlanCalcScoreConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(otherActivity);

        config.qsim().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        config.parallelEventHandling().setNumberOfThreads(16);
        config.qsim().setUsingThreadpool(false);


        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        // Scenario //chose between population file and population creator in java
        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);


        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansFile);

        //alternatively create population from data


        // Initialize controller
        final Controler controler = new Controler(scenario);


        // Run controller
        controler.run();


        // Return collected travel times

    }


}
