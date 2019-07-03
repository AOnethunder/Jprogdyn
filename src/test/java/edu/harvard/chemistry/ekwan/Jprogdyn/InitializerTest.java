package edu.harvard.chemistry.ekwan.Jprogdyn;

import java.util.*;
import java.lang.Math;
import org.apache.commons.math3.geometry.euclidean.threed.*;
import com.google.common.collect.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This test generates thermal initializations from a file and creates some
 * Gaussian input files from them.
 *
 * To run: mvn -Dtest=InitializerTest test
 */
public class InitializerTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public InitializerTest( String testName )
    {
        super( testName );
    }

    /**
     * 
     */
    public void testInitializer()
    {
        // displacement parameters
        String moleculeFilename = "test_files/methane_b3lyp_midix.out";        // filename to read modes from
        double temperature = 298.0;                                            // in K
        int numberOfInitializations = 10;                                      // how many files to make
        Map<Integer,Initializer.VibrationalInitializationType> specialModeInitializationMap = new HashMap<>();  // zero-indexed mode number --> initialization type

        // Gaussian parameters
        String outputPrefix = "analysis/methane";
        String outputSuffix = "b3lyp-midix";
        int processors = 4;
        int memory = 3; // in GB
        String routeCard = "#p b3lyp midix";
        String footer = "\n";

        // read molecule
        System.out.println("Loading data...\n");
        GaussianOutputFile frequenciesOutputFile = new GaussianOutputFile(moleculeFilename);
        Molecule molecule = frequenciesOutputFile.molecule;
        System.out.printf("Molecule read from %s:\n", moleculeFilename);
        System.out.println(molecule);
        System.out.println();
		
        // make dummy dynamics method
        CalculationMethod dynamicsMethod = new GaussianCalculationMethod(CalculationMethod.CalculationType.ENERGY_AND_FORCE,
                                                                         memory, processors, routeCard, footer);

        // make Initializer object
        Initializer initializer = new Initializer(molecule,                                                  // the molecule with frequencies to initialize with
												  temperature,                                               // in K
												  1.0, 			                                             // timestep in fs (irrelevant here)
												  Initializer.VibrationalInitializationType.QUASICLASSICAL,  // default vibrational initialization type
                                                  Initializer.RotationalInitializationType.NONE,             // no need for rotations here
                                                  specialModeInitializationMap,                              // treat some modes differently as specified here
                                                  0.1,                                                       // harmonic tolerance in percent (irrelevant)
                                                  dynamicsMethod,                                            // dynamics method (irrelevant)
                                                  1.0);                                                      // frequency scaling factor (irrelevant)

        // generate input files
        for (int i=0; i < numberOfInitializations; i++) {
            // generate a new initialization
            Molecule newMolecule = initializer.generateStructure(molecule);

            // write out molecule
            StringBuilder s = new StringBuilder();
            s.append(String.format("%%mem=%dGB\n", memory));
            s.append(String.format("%%nprocshared=%d\n", processors));
            s.append(routeCard + "\n\ntitle\n\n");
            s.append(String.format("%d %d\n", molecule.charge, molecule.multiplicity));
            for (int j=0; j < molecule.contents.size(); j++) {
                String symbol = newMolecule.contents.get(j).symbol;
                double x = newMolecule.contents.get(j).position.getX();
                double y = newMolecule.contents.get(j).position.getY();
                double z = newMolecule.contents.get(j).position.getZ();
                s.append(String.format("   %-5s     %15.10f    %15.10f    %15.10f\n", symbol, x, y, z));
            }
            if ( footer.trim().length() > 0 )
            	s.append(footer + "\n");
        	s.append("\n\n");
            String filename = String.format("%s-init_%03d-%s.gjf", outputPrefix, i, outputSuffix);
            InputFileFormat.writeStringToDisk(s.toString(),filename);
            System.out.printf("> Wrote to %s.\n", filename);
        }

        assertTrue( true );
    }
}
