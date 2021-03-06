# Jprogdyn

*quasiclassical and classical dynamics with Gaussian*

## Contents
  - [Introduction](#introduction)
    - [Features](#features)
    - [Installation](#installation)
    - [Javadoc](#javadoc)

  - [Tutorials](#tutorials)
   - [Reaction Trajectory Tutorial](#reaction-trajectory-tutorial)
   - [NMR Trajectory Tutorial](#nmr-trajectory-tutorial)
   
  - [Notes](#notes)

  - [Fine Print](#fine-print)

## Introduction

*Jprogdyn* is an open-source Java program for running quasiclassical and classical dynamics calculations using the Gaussian electronic structure program.  *Jprogdyn* uses the normal modes from a standard Gaussian `freq` calculation to sample an initial structure from a given thermodynamic ensemble and calculates a trajectory using the [Velocity Verlet algorithm](https://en.wikipedia.org/wiki/Verlet_integration#Velocity_Verlet).  These trajectories can be used to study the free energy surfaces of reactions or calculate rovibrational corrections to NMR shieldings.

### Features

* compatible with Gaussian '09 and '16
* reaction trajectories
* rovibrational corrections to NMR shieldings
* quasiclassical or classical sampling of vibrations
* classical sampling of rotations
* arbitrary temperature
* parallel trajectories on one computer

### Installation

*Jprogdyn* requires Java 1.8 or later.  [Apache Maven](https://maven.apache.org/) is required to build *Jprogdyn*.  A working copy of [Gaussian](gaussian.com) and a Linux-like (Mac, Cygwin, Unix, or actual Linux) environment is required to run *Jprogdyn*.

The installation process is as follows:

1. Download and install [the Java Development Kit](https://www.oracle.com/technetwork/java/javase/downloads/index.html), [Maven](https://maven.apache.org/download.cgi), and [Gaussian](gaussian.com).
2. Clone this repository: `git clone https://github.com/ekwan/Jprogdyn.git`
3. Build the package: `mvn package`  This places the compiled classes in the `target` folder, but you should not have to manipulate the files there directly.
4. The `run_gaussian.sh` script in `Jprogdyn/gaussian` folder is the interface between *Jprogdyn* and the version of Gaussian on your system.  Please edit this script accordingly according to your system's requirements.  For example, you can adjust the scratch folder (`GAUSS_SCRDIR`) or the name/path of the Gaussian executable.

That's it!  If you encounter any problems during the build process, please let us know and we'll be glad to help you.

### Javadoc

*Jprogdyn* is open-source software.  To facilitate modifications to the code, there is an extensive javadoc.  To build the javadoc:

`mvn javadoc:javadoc`

This will place the result in:

`Jprogdyn/target/site/apidocs/edu/harvard/chemistry/ekwan/Jprogdyn/package-summary.html`

## Tutorials

Configuration files for these tutorials are stored in the `tutorials/` folder.

### Running *Jprogdyn*

The behavior of *Jprogdyn* is controlled by a configuration file (by default, `Jprogdyn.config`).  *Jprogdyn* operates trajectories in two modes, called `job_type`:

- `job_type : reaction` means to run a reaction trajectory.  This means that no NMR shieldings will be calculated.  Trajectories that reach predefined geometric conditions (e.g., bonds lengths matching the product) will be stopped before completing.
- `job_type : nmr` means to run a trajectory for the purpose of calculating the rovibrational correction to the NMR shieldings.  NMR shieldings will be calculated every `nmr_interval` points and trajectories will not be stopped before they have reached the specified `number_of_forward_points` and `number_of_backward_points`.

*Jprogdyn* does its work in two phases:

- `job_type : trajectory` means to run trajectories
- `job_type : analysis` means to analyze the trajectories by loading in `.chk` files (please see below).

In some cases, it may be possible to analyze incomplete trajectories, but we generally don't recommend this unless you know what you're doing.

*Jprogdyn* will run up to `number_of_simultaneous_trajectories` at once on one node.  Please use your local computing cluster queueing system to run trajectories on more than one node at a time.  The progress of each trajectory is saved every `checkpoint_interval` points.  To restart jobs, simply re-run the same configuration file that generated the interrupted trajectories.  Note that each trajectory is saved to a `.chk` file, but the previous checkpoint is always backed up to a `.chk.bak` file so that an uncorrupted checkpoint is always available if *Jprogdyn* is interrupted while writing to a checkpoint.  You can safely delete these files when the trajectories are complete.

There are a lot more options in the configuration file.  More details for some options are given below and in comments in the configuration file itself.

### Reaction Trajectory Tutorial

In this tutorial, we reproduce the quasiclassical trajectory analysis in our [study](#ref1) of nucleophilic aromatic substitution reactions.  

<img src="img/reaction.png" height=100>

This reaction was studied at B3LYP/6-31+G*/PCM(DMF) and found to be concerted on the potential energy surface, but pseudo-stepwise on the free energy surface.

On an Intel node with 36 cores and 128 GB from 2017, the four trajectories (1000 fs each) took about 30 hours to run.  (See the section on [termination conditions](#termination-conditions) to stop the trajectories when they have reached a defined structure.  This tutorial file does not have any termination conditions set, but they can be added, and will speed up the trajectory calculations a great deal.)

1. **Gather Files**

 You only need one Gaussian file to run this job: the transition state (on the potential energy surface) for this S<sub>N</sub>Ar reaction.  Generally all trajectories should be initialized from stationary points.  If you don't, the harmonic approximation will be poor, and it won't be possible to initialize the trajectories properly.  Please ensure you are running dynamics at the exact same level of theory that you found your transition state at.  (This is not checked!)

 *Stationary Geometry*: Fluoride + 2,4-dinitro-1-chlorobenzene (stored in `test_files/dinitro_Cl_F-1H2O-ts-b3lyp_d3bj-631+gd-dmf_pcm.out`).  The route card used was:
 
 ```
#p opt=(calcfc,ts,noeigentest) freq=(noraman,hpmodes) b3lyp scrf=(solvent=n,n-dimethylformamide,pcm) empiricaldispersion=gd3bj 6-31+g*`
```

 Please ensure you use the `freq=hpmodes` in your frequency files.

2. **Run Trajectories**

 To run the trajectories, type `mvn exec:java -Dconfig.filename="tutorials/reaction_tutorial_trajectories.config"`.  (You may want to place this command inside of a script so that it can dispatched to a job queueing system.)
 
 The following refer to entries within the configuration file:

 - `frequency_file : dinitro_Cl_F-1H2O-ts-b3lyp_d3bj-631+gd-dmf_pcm.out`  This is the file containing the structure to initialize the dynamics from.  It should contain one (and only one) `freq=hpmodes` job.
 - Please adjust `number_of_simultaneous trajectories`, `number_of_processors_per_trajectory`, and `memory_per_trajectory` as appropriate for your system.
 - `gaussian_force_route_card`: the route card to use for propagating trajectories with single-point Gaussian force calculations.  Notice `#p` should not be entered here.
 - `gaussian_force_footer` is blank, but this would be the place to put any special basis sets or other directives.
 - `checkpoint_prefix` is what the checkpoints filenames for each trajectory will start with.
 - `vibrational_initialization_default : quasiclassical` and `vibrational_initialization_override : 0:ts_positive` mean to initialize all normal modes quasiclasically and, only for mode 0, initialize with no displacement and give the reaction forward velocity in the transition mode.  The sign of the transition mode is arbitrary and depends on which geometry you define as products (see the discussion on [transition modes](#transition-modes) below).
 - `rotational_initialization_type : classical` means to start rotating the molecule a small amount.
 - `termination_condition : no_termination_conditions` means to let the trajectories run for the complete time (i.e., run all forward and backward points).  These conditions can also used for the analysis step (see below) and tell *Jprogdyn* which internal coordinates to analyze in the next ste.

3. **Analyze Trajectories**

 To analyze the trajectories, type `mvn exec:java -Dconfig.filename="tutorials/reaction_tutorial_analysis.config"`.
 
4. **Examine Results**

 - The "trajectory stability analysis" shows some statistics about the variability of the total energy in each trajectory:

```
=== Trajectory Stability Analysis ===

Trajectory                                         points      Std. Dev. (%)       Std. Dev. (kcal)
                                                                               0.0                  5.0
SNAr_tutorial_0000.chk                             1000         0.000079%       [  *                 ] 0.6
SNAr_tutorial_0001.chk                             1000         0.000063%       [ *                  ] 0.5
SNAr_tutorial_0002.chk                             1000         0.000070%       [  *                 ] 0.6
SNAr_tutorial_0003.chk                             1000         0.000056%       [ *                  ] 0.4
```

 The standard deviation of each trajectory is shown as an `AsciiBar`.  The standard deviation in kcal for each trajectory is shown to the right of the square brackets.  The asterisk in each `AsciiBar` marks the approximate standard deviation, with `0.0` and `5.0` indicating the limits of the scale.  In general, the standard deviation should be less than 0.0001%.  We recommend discarding any trajectories with unusually high standard deviations.  You can do so simply by deleting the `.chk` and `.chk.bak` files associated with the bad trajectories and re-running the same input file as you used to run the trajectories.
 
 - The "geometry analysis" shows key geometric parameters as a function of time for each trajectory.  Here is an example of a trajectory that went to product:

 ```
SNAr_tutorial_0001.chk (complete)

Time(fs)        C-F                             C-Cl

 -500      [                 *  ] 8.41     [*                   ] 1.76
 -400      [                 *  ] 8.42     [*                   ] 1.77
 -300      [              *     ] 7.21     [*                   ] 1.67
 -200      [           *        ] 5.97     [*                   ] 1.87
 -100      [     *              ] 3.62     [*                   ] 1.70
    0      [ *                  ] 1.84     [*                   ] 1.76
  100      [*                   ] 1.40     [*                   ] 1.85
  200      [*                   ] 1.45     [*                   ] 2.00
  300      [*                   ] 1.30     [   *                ] 3.02
  400      [*                   ] 1.37     [        *           ] 5.09
  500      [*                   ] 1.47     [             *      ] 6.83
  500      [*                   ] 1.47     [             *      ] 6.83
product reached after 356 fs.
```
 
 - It is also possible for a trajectory to recross.  In this example, recrossing from product to product was found:

```
SNAr_tutorial_0000.chk (complete)

Time(fs)        C-F                             C-Cl

 -500      [*                   ] 1.26     [           *        ] 5.99
 -400      [*                   ] 1.35     [     *              ] 3.85
 -300      [*                   ] 1.38     [ *                  ] 2.22
 -200      [*                   ] 1.54     [*                   ] 1.88
 -100      [*                   ] 1.47     [*                   ] 1.86
    0      [ *                  ] 1.85     [*                   ] 1.82
  100      [*                   ] 1.41     [*                   ] 1.83
  200      [*                   ] 1.40     [ *                  ] 2.36
  300      [*                   ] 1.41     [       *            ] 4.43
  400      [*                   ] 1.27     [            *       ] 6.54
  500      [*                   ] 1.33     [                  * ] 8.68
  500      [*                   ] 1.33     [                  * ] 8.68
product reached after 284 fs.
```

 - Statistics on all outcomes are also shown:

```
=== Summary of Trajectories ===

Species                       Trajs          %
starting_material                 0          0
product                           4        100
unknown (incomplete)              0          0
unknown (complete)                0          0

4 trajectories total.
```

 - If requested, `.traj` files will be written to the `analysis/` folder, with one `.traj` file per trajectory.  These movies can be played with MOLDEN.

 `Wrote internal coordinate data to share_root/ekwan/Jprogdyn/analysis/SNAr_tutorial_0000.csv.`

 - `.csv` files containing key bond lengths, angles, or torsions for each trajectory can also be written to disk.  You can control this behavior using the `analysis_coordinate` keyword.  To turn this off, set `write_analysis_csv` to `no`.
 
```
time,C-F,C-Cl
-500.00,1.2585,5.9894
-499.00,1.2775,5.9754
-498.00,1.3012,5.9590
```
 
 Data like this can be processed in many standard programs like Excel.
 
 - Note that the `termination_conditions` section has been populated (and it wasn't for the running of the trajectories).  This defines a set of reference conditions so that *Jprogdyn* can identify whether trajectories reached starting material, product, or some other known state.

 - Note that it is possible to analyze trajectories before they are finished. There should be no ill effects on any running trajectories.


### NMR Trajectory Tutorial

In this tutorial, we reproduce the rovibrational correction to the NMR shifts of methane reported in our [2015 study](#ref2).  We will compute trajectories on the B3LYP/MIDI! surface, calculate NMR shieldings every 8 points using B3LYP/cc-pVDZ, and analyze the results to obtain the raw rovibrational corrections.  The expected values are: -4.22 ppm (<sup>13</sup>C) and -0.64 ppm (<sup>1</sup>H).

On an Intel node with 36 cores and 128 GB from 2017, the 25 trajectories (250 fs each) took about 3 hours to run.

 1. **Gather Files**

 The test files are provided in the `test_files` folder:

 *Stationary Geometry*: Methane optimized with the route card `#p b3lyp/midix opt freq=(noraman,hpmodes)`, stored in `methane_b3lyp_midix.out`.

 *Shieldings*: The geometry from the previous file run with the route card `#p b3lyp/cc-pvdz NMR`, stored in `methane_b3lyp_midix_NMR_b3lyp_dz.out`.

 If you want to calculate another system, please **double-check** that you are using the same geometry for both files, the dynamics level of theory matches the level of theory for the stationary geometry, and the NMR level of theory matches the level of theory for the shieldings file.  These criteria are not checked by *Jprogdyn* and errors will render the results completely meaningless.

2. **Run Trajectories**

 The job has been setup in `tutorials/NMR_tutorial_trajectories.config`.  The following refer to entries within that file:

 - `trajectory_type : nmr` and `job_type : trajectory` requests that NMR trajectories be run.

 - Filenames of the Gaussian files are given in `frequency_file` and `shieldings_file`.

 - Please adjust `number_of_simultaneous trajectories`, `number_of_processors_per_trajectory`, and `memory_per_trajectory` as appropriate for your system.

 - You can increase `number_of_trajectories` if desired to get more precision.

 To run the job, go to the *Jprogdyn* directory and type: `mvn compile` then `mvn exec:java -Dconfig.filename="tutorials/methane_NMR_trajectories.config"`.

 This job should take approximately 1-2 hours on a modern machine.  If the individual (non-NMR) points (`eval time`) are taking more than 30 seconds, that would be abnormally slow.  We'd be glad to help you if you're having 
trouble.

 The trajectories will be written to the `checkpoints/` folder as files like `methane_0000.chk`.  If the job gets interrupted, you can restart it simply by re-running it.  The trajectories will pick up from where they left off automatically.

3. **Analyze Trajectories**

 The job has been setup in `tutorials/methane_NMR_analysis.config`.  The following refer to entries within that file:

 - `trajectory_type : nmr` and `job_type : trajectory` requests that an analysis of existing trajectories be run.

 - The checkpoints are assumed to be in `checkpoint_directory` and all `.chk` files starting with `checkpoint_prefix` will be read in.

 - Incomplete trajectories will be ignored.

 - Some additional information regarding the stability of the total energy and internal coordinates is provided.  Movies that can be viewed in MOLDEN are written as `.traj` files to the `analysis/` folder.  CSV files of some sample internal coordinates over time are written to the same location.

 To run the job, type `mvn exec:java -Dconfig.filename="tutorials/methane_NMR_analysis.config"`.  (There is no need to recompile.)

4. **Examine Results**

 The last part of the output file will provide the raw corrections:

```
=== NMR Raw Correction Analysis ===

Analyzing 25 complete trajectories...
25 total trajs
Element:  Atom Numbers:                   Raw Correction:     Std. Err.:      Std. Dev.:
   H        2   3   4   5                      -0.67            0.010           0.05
   C        1                                  -4.36            0.218           1.09
```

 Because these numbers were derived from a limited number of stochastic simulations, the numbers differ slightly from the expected ones.  The standard error bar is the raw correction plus or minus the standard error.

 To calculate the predicted NMR shift, you will also need to calculate a raw correction for the reference (e.g. TMS) as well as the stationary shieldings for the molecule at a high level of theory.  Alternatively, you can calculate a series of compounds and linearly scale your absolute shieldings to experimental values.  Please see [reference 2](#ref2) for details.

## Notes

This section describes some of the details of how Jprogdyn works and some of the potential issues that can arise.

### Input

Both reaction and NMR trajectories are initialized from the result of a Gaussian `freq=hpmodes` calculation.  (The extra precision from `hpmodes` is helpful for getting consistent results.)  Please ensure that the starting structure is a true stationary point on the dynamics surface.  Please also ensure that there is only one frequency job per file.  Small imaginary frequencies (e.g., due to DFT integration grid inaccuracies) should be avoided, but probably will not influence the results heavily.

### Quasiclassical Initialization

In quasiclassical initialization, each normal mode is randomly placed in a quantum harmonic oscillator eigenstate, with a probability that is appropriate to the specified temperature.  This determines the amount of total energy in the mode.  The displacement is chosen from the probability density function for the given eigenstate, which determines the partitioning between potential and kinetic energy for the mode.  For all but the softest modes, the most likely eigenstate by far is the ground state (at room temperature).

### Classical Initialization

Classical initialization uses the probability for a classical, rather than quantum mechanical, harmonic oscillator.  In general, quasiclassical trajectories are more realistic, but classical trajectories can be propagated longer (see section on Propagation below).

### Transition Modes

When initializing dynamics from a transition state to study a reaction, the velocity in the transition mode (i.e., the imaginary frequency) is generally set to be "forward."  Whether "forward" corresponds to a positive or negative sign in the transition mode is arbitrary.  You can determine this by animating the imaginary mode.  In GaussView, you can animate the imaginary mode by going to Results...Vibrations and using the "manual displacement" tool.  By moving the slider to the right, you can determine whether a positive displacement corresponds to a forward or backwards reaction.  In general, all reactions will have some degree of recrossing: some of these "forward" trajectories will end up at starting material.  Some reactions, such as the ones Professor Singleton and many others are interested in, have much more recrossing.  A full discussion of recrossing is beyond the scope of this file.

### Rotational Initialization

The molecule can also have rotational energy added to it from a classical distribution.  In principle, this could result in some centrifugal distortion that can alter the results.  In practice, we have found this makes very little difference.

### Initialization Tolerance

The frequencies provided to the initialization routine constitute a harmonic approximation to the potential energy surface.  In the stiffer modes (e.g., C-H stretches), this approximation is excellent in that displacing the molecule along the normal mode will increase the potential energy by the expected amount.  In softer modes (i.e., low frequencies), the surface may be much more anharmoic.

*Jprogdyn* always initializes trajectories harmonically, in that the predicted potential energy and the actual potential energy of the initially displaced structure must agree to within the specified tolerance level (specified in percent as a total of the absolute energy).  In some cases, it may be necessary to disable displacements in the lower frequency modes to prevent the initialization routine from straying into very anharmonic territory.  Note that the fact that *Jprogdyn* trajectories always start in harmonic regions of the potential energy surface does not preclude the trajectories from exploring anharmonic regions as they propagate.

### Propagation

Trajectories are propagated using the Velocity Verlet algorithm.  From Wikipedia:

<img src="img/velocity_verlet_position.svg" height=40>

<img src="img/velocity_verlet_velocity.svg" height=40>

In general, a timestep of 1.0 fs is a good compromise between speed (fewer trajectory points) and accuracy (avoiding integration error).  Quasiclassical trajectories should not be extended more than ~500 fs forwards or backwards, due to unphysical intramolecular vibrational redistribution.

### Termination Conditions

When running NMR trajectories, simulations should be run both forwards and backwards for the full 125 fs in both directions.  This ensures proper sampling of the vibrational correction.  Set `termination_conditions : no_termination_conditions` to request this behavior.

When running reaction trajectories, it can be helpful to run full trajectories (e.g., 500 fs forwards and 500 fs backwards) when doing exploratory work.  However, once a trajectory reaches a minimum (e.g., the product) it usually does not leave, so many points may be wasted.  To define termination conditions for the S<sub>N</sub>Ar tutorial:

```
termination_condition : bond_length, 8, 13, C-F, greater_than, 3.0
termination_condition : bond_length, 8, 17, C-Cl, greater_than, 4.0
```

This stops the trajectories once the C-F bond distance is greater than 3.0 A (meaning this is a starting material structure) or the C-Cl bond distance is greater than 4.0 A (meaning this is a product structure).  (It is possible to end up at starting material, even if the trajectories are initialized in the forward direction, because of recrossing.)

In `job_type : trajectory` mode, forward points are run, and then backward points.  If the termination conditions are reached in the forward direction, the trajectory will be stopped, and no further points will be evaluated.  The termination conditions are not checked for backward points.  Note that it is currently not possible to join termination conditions together.  It would be possible to alter the `checkTerminationConditions` method in `Trajectory.java` to accomplish this as a temporary hack.

In `job_type : analysis` mode, these termination conditions are used to label the outcomes of the trajectories.  You could change `C-F` to `starting_material` to have a more descriptive report.  It is also not possible to join termination conditions for the analysis, but the `analyzeGeometry` method in `TrajectoryAnalyzer.java` takes lists of conditions.  Every condition in each list must be met for its corresponding condition to be declared as reached.  You could play with this, but it might be easier to dump out a CSV of the internal coordinates and analyze them in a third-party program.

### Visualizing Trajectories

You can visualize the trajectories with [MOLDEN](http://cheminf.cmbi.ru.nl/molden/).  Run *Jprogdyn* in analysis mode and set `make_molden_movies` to `yes`.  One `.traj` file per trajectory will be written to the `analysis_directory` folder.  These can be opened directly by MOLDEN.

### Parallel Execution

*Jprogdyn* has facilities to run trajectories on one computer simultaneously.  To run *Jprogdyn* on multiple computers at once, compile *Jprogdyn* once and run the program using different `.config` files.  Choose the filenames and directories in these `.config` files so that the simulataneous instances of *Jprogdyn* will not confict.  Avoid recompiling the classes while *Jprogdyn* is in operation.

### Other Electronic Structure Programs

*Jprogdyn* has been written to interface with Gaussian, but it should be possible to modify it to work with other electronic structure programs as long as the same information (forces, frequencies, normal modes, reduced masses, etc.) is available.

## Fine Print

### Acknowledgements

This program is inspired by the `progdyn` suite of software written by Professor Daniel A. Singleton.  E.E.K. would like to express his gratitude to Professor Singleton for his mentorship and support over many years.

### References

1. <a id="ref1" href="https://doi.org/10.1038/s41557-018-0079-7">"Concerted Nucleophilic Aromatic Substitutions."</a>  Kwan, E.E.; Zeng, Y.; Besser, H.A.; Jacobsen, E.N.  *Nature Chem.* **2018**, *10*, 917&ndash;923.

2. <a id="ref2" href="https://doi.org/10.1021/acs.jctc.5b00856">"Enhancing NMR Prediction for Organic Compounds Using Molecular Dynamics."</a>  Kwan, E.E.; Liu, R.Y.  *J. Chem. Theory Comput.*  **2015**, *11*, 5083&ndash;5089.

### Authors

*Jprogdyn* was written by Eugene Kwan and Richard Liu at the Department of Chemistry and Chemical Biology at Harvard University.  The name simply means "Java program for dynamics."

### Getting Help

Please email `ekwan16@gmail.com` with any questions.  We will gladly try to help you.

## How to Cite

Please cite [reference 2](#ref2).

## License
   
This project is licensed under the Apache License, Version 2.0. See `LICENSE.txt` for full terms and conditions.
   
*Copyright 2018 by Eugene E. Kwan and Richard Y. Liu*
