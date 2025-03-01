/**
 * Aitken Basin Federate.
 *
 * This application is a High Level Architecture (HLA) federate used to
 * publish the location of the Aitken Basin local lunar reference frame.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
/**
 * Copyright 2015 Edwin Z. Crues
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package see.smackdown;

// Java signal handling classes.
import sun.misc.Signal;
import sun.misc.SignalHandler;

import see.smackdown.environment.EnvironmentHLA;
// Smackdown environment classes.
import see.smackdown.reference_frame.*;
import see.smackdown.utilities.*;

// Java utility classes.
import java.util.prefs.Preferences;


// JArgs classes.
import jargs.gnu.CmdLineParser;


class AitkenBasin {

   // Executive loop control variables.
   boolean     continue_execution = true;  // Execution loop control flag.
   long        exec_loop_counter  = 0;     // Cumulative loop counter.
   static long end_count = Long.MAX_VALUE; // Simulation termination count.
   
   // Simulation time parameters.
   static final long USEC_PER_CYCLE = 1000000; // Microseconds per exec cycle.
   double            sim_exec_time = 0.0;      // Simulation time in seconds.
   double            time_epoch;               // Start time for simulation.
   double            time;                     // Current time.
   double            end_time;                 // End time for simulation.
   boolean           enable_realtime = false;  // Enable realtime control.

   // Shutdown protection flag.
   private boolean shutdown_called = false;

   // Declare the environmental reference frames of interest.
   private ReferenceFrame sun_inrtl_frame;
   private ReferenceFrame em_bary_inrtl_frame;
   private ReferenceFrame em_bary_rot_frame;
   private ReferenceFrame earth_inrtl_frame;
   private ReferenceFrame earth_pfix_frame;
   private ReferenceFrame moon_inrtl_frame;
   private ReferenceFrame moon_pfix_frame;
   private ReferenceFrame em_L2_frame;
   private ReferenceFrame mars_inrtl_frame;
   private ReferenceFrame mars_pfix_frame;

   // Declare the new Aitken Basin local fixed frame.
   private ReferenceFrame aitken_basin_frame;

   // Declare the Environment federate ambassador.
   private EnvironmentHLA env_hla;
   private static String  crc_host;
   private static int     crc_port;
   private static String  federate_name;

   // Verbose output control flag.
   private static boolean verbose_output = false;

   // Declare the "Main" routine for the application.
   public static void main( String[] args )
   {

      // Print out a start up message.
      System.out.println();
      System.out.println("*** Aitken Basin Frame Federate ***");
      System.out.println();

      // Get the preferences node for this application class.
      AitkenBasin.process_preferences();

      // Parse the command line options; these will override preferences.
      AitkenBasin.process_command_line_options( args );

      // Instantiate a AitkenBasin instance.
      final AitkenBasin ab_fed = new AitkenBasin();

      // Register an signal handler for Ctrl-C.
      SignalHandler handler = new SignalHandler () {
         public void handle(Signal sig) {
            System.out.println("In signal handler.");
            ab_fed.continue_execution = false;
         }
      };
      Signal.handle(new Signal("INT"), handler);

      // Catch any exceptions that might be generated in the execution loop.
      try {

         // Configure the AitkenBasin
         ab_fed.configure();

         // Initialize the AitkenBasin.
         if ( ab_fed.initialize() == 0 ) {

            // Print out connection information.
            System.out.println( "************************************************" );
            System.out.println( "CRC host: " + crc_host );
            System.out.println( "CRC port: " + crc_port );

            // If we're at the run loop then go ahead and save the preferences.
            store_preferences();

            // Execute the run loop.
            ab_fed.run();

         }

         // Execution is finished so shutdown.
         ab_fed.shutdown();

      } catch (Exception e) {

         // Bad Scoobees so try to see what happened.
         e.printStackTrace();

      }

   }
   
   // Process the preferences.
   private static void process_preferences()
   {
      // Instantiate the application preferences.
      Preferences prefs = Preferences.userNodeForPackage( AitkenBasin.class );

      // Check for federate name preference.
      federate_name = prefs.get( "aitken_basin_federate_name",
                                 "Aitken Basin Frame" );

      // Check for CRC host location preference.
      crc_host = prefs.get( "CRC_host", "localhost" );

      // Check for CRC port preference.
      crc_port = prefs.getInt( "CRC_port", 8989 );

      return;
   }
   
   
   // Process the preferences.
   private static void store_preferences()
   {
      // Instantiate the application preferences.
      Preferences prefs = Preferences.userNodeForPackage( AitkenBasin.class );

      // Save the federate name preference.
      prefs.put( "aitken_basin_federate_name", federate_name );

      // Save the CRC host location preference.
      prefs.put( "CRC_host", crc_host );

      // Save the CRC port location preference.
      prefs.putInt( "CRC_port", crc_port );

      return;
   }


   // Process the command line options.
   private static void process_command_line_options( String[] args )
   {

      // Instantiate the command line argument parser.
      ABFedOptionsParser options = new ABFedOptionsParser();

      // Parse the options.
      try {
         options.parse(args);
      }
      catch ( CmdLineParser.UnknownOptionException e ) {
          System.err.println(e.getMessage());
          print_usage();
          System.exit(2);
      }
      catch ( CmdLineParser.IllegalOptionValueException e ) {
          System.err.println(e.getMessage());
          print_usage();
          System.exit(2);
      }

      // Check for help option.
      Boolean h_opt = (Boolean)options.getOptionValue( ABFedOptionsParser.HELP );
      if ( h_opt != null ) {
         if ( h_opt ) {
            print_usage();
            System.exit(0);
         }
      }

      // Check for verbose output option.
      Boolean v_opt = (Boolean)options.getOptionValue( ABFedOptionsParser.VERBOSE );
      if ( v_opt != null ) {
         if ( v_opt ) {
            verbose_output = true;
         }
      }

      // Check for override of federate name.
      String n_opt = (String)options.getOptionValue( ABFedOptionsParser.FED_NAME );
      if ( n_opt != null ) {
         federate_name = new String( n_opt );
      }

      // Check for override of CRC host location.
      String host_opt = (String)options.getOptionValue( ABFedOptionsParser.CRC_HOST );
      if ( host_opt != null ) {
         crc_host = new String( host_opt );
      }

      // Check for override of CRC port.
      Integer port_opt = (Integer)options.getOptionValue( ABFedOptionsParser.CRC_PORT );
      if ( port_opt != null ) {
         crc_port = port_opt;
      }

      // Check for setting simulation run time.
      Integer run_time_opt = (Integer)options.getOptionValue( ABFedOptionsParser.RUN_TIME );
      if ( run_time_opt != null ) {
         end_count = (long)((run_time_opt * 1000000.0)/(double)USEC_PER_CYCLE);
      }

      return;
   }


   private static class ABFedOptionsParser extends CmdLineParser {

      // Set help message option.
      public static final Option HELP = new
         CmdLineParser.Option.BooleanOption( 'h', "help" );

      // Set verbose output.
      public static final Option VERBOSE = new
         CmdLineParser.Option.BooleanOption( 'v', "verbose" );

      // Set the run time in for the simualtion in integer seconds.
      public static final Option RUN_TIME = new
         CmdLineParser.Option.IntegerOption( 'r', "run_time" );

      // Set the name of this HLA federate.
      public static final Option FED_NAME = new
         CmdLineParser.Option.StringOption( 'n', "name" );

      // Set the host name where the CRC/RTI is running.
      public static final Option CRC_HOST = new
         CmdLineParser.Option.StringOption( "crc_host" );

      // Set the port number to communicate with the CRC/RTI host.
      public static final Option CRC_PORT = new
         CmdLineParser.Option.IntegerOption( "crc_port" );

      // Deafult constructor.
      public ABFedOptionsParser() {
         super();
         addOption( HELP );
         addOption( VERBOSE );
         addOption( RUN_TIME );
         addOption( FED_NAME );
         addOption( CRC_HOST );
         addOption( CRC_PORT );
      }
   }


   // Usage message for command line argument errors.
   private static void print_usage() {
       System.err.println( "usage: AitkenBasin:" );
       System.err.println( "          [{-h,--help}]" );
       System.err.println( "          [{-v,--verbose}]" );
       System.err.println( "          [{-r,--run_time} seconds]" );
       System.err.println( "          [{-n,--name} federate_name]" );
       System.err.println( "          [{--crc_host} CRC_host_name]" );
       System.err.println( "          [{--crc_port} CRC_port_number]" );
   }


   // AitkenBasin constructor
   private AitkenBasin()
   {

      // Instantiate the Environment HLA interface.
      env_hla = new EnvironmentHLA( federate_name, 1.0 );

      // Instantiate the Federation reference frames.
      sun_inrtl_frame   = new ReferenceFrame( "SunCentricInertial");
      em_bary_inrtl_frame=new ReferenceFrame( "SolarSystemBarycentricInertial",
                                              "EarthMoonBarycentricInertial"  );
      em_bary_rot_frame = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                              "EarthMoonBarycentricRotating" );
      earth_inrtl_frame = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                              "EarthCentricInertial"         );
      earth_pfix_frame  = new ReferenceFrame( "EarthCentricInertial",
                                              "EarthCentricFixed"    );
      moon_inrtl_frame  = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                              "MoonCentricInertial"          );
      moon_pfix_frame   = new ReferenceFrame( "MoonCentricInertial",
                                              "MoonCentricFixed"     );
      em_L2_frame       = new ReferenceFrame( "EarthMoonBarycentricInertial",
                                              "EarthMoonL2Rotating"          );
      mars_inrtl_frame  = new ReferenceFrame( "SolarSystemBarycentricInertial",
                                              "MarsCentricInertial"          );
      mars_pfix_frame   = new ReferenceFrame( "MarsCentricInertial",
                                              "MarsCentricFixed"     );

      // Instantiate the new Aitken Basin local fixed frame.
      aitken_basin_frame = new ReferenceFrame( "MoonCentricFixed",
                                               "AitkenBasinLocalFixed" );

      // Inform the EnvironmentFederateAmbassador of expect reference frames.
      env_hla.add_expected_frame( sun_inrtl_frame,
                                  sun_inrtl_frame.ref_frame_name );
      env_hla.add_expected_frame( em_bary_inrtl_frame,
                                  em_bary_inrtl_frame.ref_frame_name );
      env_hla.add_expected_frame( em_bary_rot_frame,
                                  em_bary_rot_frame.ref_frame_name );
      env_hla.add_expected_frame( earth_inrtl_frame,
                                  earth_inrtl_frame.ref_frame_name );
      env_hla.add_expected_frame( earth_pfix_frame,
                                  earth_pfix_frame.ref_frame_name );
      env_hla.add_expected_frame( moon_inrtl_frame,
                                  moon_inrtl_frame.ref_frame_name );
      env_hla.add_expected_frame( moon_pfix_frame,
                                  moon_pfix_frame.ref_frame_name );
      env_hla.add_expected_frame( em_L2_frame,
                                  em_L2_frame.ref_frame_name );
      env_hla.add_expected_frame( mars_inrtl_frame,
                                  mars_inrtl_frame.ref_frame_name );
      env_hla.add_expected_frame( mars_pfix_frame,
                                  mars_pfix_frame.ref_frame_name );


   }


   private void configure()
   {

      // Configure the HLA interface to connect to host and port.
      env_hla.configure( crc_host, crc_port );
      
      // Don't try to create the federation.
      env_hla.create_federation = false;
      
      // Publish and subscribe to RefeneceFrame attributes.
      env_hla.publish_reference_frames = true;
      env_hla.subscribe_reference_frames = true;

   }


   private int initialize()
   {

      if ( env_hla.initialize( true, false ) != 0 ) { return 1; }

      // Set the location of the Aitken Basin frame wrt MoonCetericFixed.
      aitken_basin_frame.position[0] = -817582.939286128;
      aitken_basin_frame.position[1] = -296194.936333636;
      aitken_basin_frame.position[2] = -1504977.52696795;
      Vector3.initialize( aitken_basin_frame.velocity );

      // Set the attitude of the Aitken Basin frame wrt MoonCetericFixed.
      aitken_basin_frame.attitude.scalar = -0.212035899134992;
      aitken_basin_frame.attitude.vector[0] = -0.79079044533773;
      aitken_basin_frame.attitude.vector[1] = 0.554597207736449;
      aitken_basin_frame.attitude.vector[2] = 0.148705030888344;
      Vector3.initialize( aitken_basin_frame.attitude_rate );

      // Compute the corresponding attitude transformation.
      aitken_basin_frame.attitude.left_quat_to_transform( aitken_basin_frame.T_parent_body );

      // Publish the Aitken Basin local reference frames.
      this.publish_frames();

      return 0;
   }


   // Register the published reference frame for this federate.
   private void publish_frames()
   {

      // Register the frames to publish.
      env_hla.add_published_frame( aitken_basin_frame );

      return;

   }

   
   // Executive run loop.
   private void run()
   {
      double sim_exec_time;

      // Advance on cycle to make sure that we have the
      // data we'll be wainting on for intialization.
      env_hla.request_time_advance();
      env_hla.wait_for_time_advance();

      // Wait on the arrival of the initialization data.
      env_hla.wait_on_initialization_data();

      // Compute the simulation termination time for informational purposes.
      end_time = (end_count * USEC_PER_CYCLE) / 1000000.0;
      
      // Set the physical time epoch from the SimConfig object in seconds.
      time_epoch = moon_inrtl_frame.time * 60 * 60 * 24;

      System.out.println( "************************************************" );
      System.out.print( "AitkenBasin \"" );
      System.out.print( federate_name );
      System.out.print( "\": Starting time (epoch): " );
      System.out.println( time_epoch );
      System.out.println( "************************************************" );

      // This is the run loop
      while ( continue_execution ) {

         // Compute the current simulation execution time.
         sim_exec_time = (exec_loop_counter * USEC_PER_CYCLE) / 1000000.0;
         
         // Compute the problem time.
         time = time_epoch + sim_exec_time;
         
         // If we're in playback mode, we need to "regulate" time.
         if ( enable_realtime ) {
            try {
               Thread.sleep( 1000 );
            } catch( Exception e ) {
               System.out.print( "AitkenBasin \"" );
               System.out.print( federate_name );
               System.out.println( "\": Unknown error in sleep:" );
               System.out.println( e.getMessage() );
            }
         }

         // Wait for the time advance grant.
         while( env_hla.wait_for_time_advance() ){
            // Use a sleep here instead of a wait to play nice with the CPU usage.
            try {
               Thread.sleep( 10 );
            } catch( Exception e ) {
               System.out.print( "AitkenBasin \"" );
               System.out.print( federate_name );
               System.out.println( "\": Unknown error in sleep:" );
               System.out.println( e.getMessage() );
            }
            //Thread.yield();
         }
         
         // Do things in this section of code.
         // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

         // Update the federation reference frames.
         this.update_environment();

         // Need to publish the frames to the federation.
         this.env_hla.update_published_frames();

         // Print out execution loop timing.
         System.out.println( "Times are:" );
         System.out.print( "   Executive Loop Counter: " );
         System.out.println( exec_loop_counter );
         System.out.print( "   Simulation Execution Time: " );
         System.out.println( sim_exec_time );
         System.out.print( "   AitkenBasin Physical Time: " );
         System.out.println( time );

         // Print out ReferenceFrame info.
         System.out.print( "   Federateration Execution Time (s): " );
         System.out.println( env_hla.get_hla_time() );
         if ( AitkenBasin.verbose_output ) {
            this.print_frames();
         }
         System.out.println();

         // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         // End of section to do things.

         // Request a time advance by the logical time interval.
         env_hla.request_time_advance();
         
         // Increment the executive loop counter.
         exec_loop_counter++;
         
         // Check for termination time.
         if ( exec_loop_counter > end_count ){ continue_execution = false; }
         
      }

      return;

   }


   // Update all the federation environmental reference frames.
   private void update_environment()
   {

      // Update the Aitken Basin local fixed refernce frames to the current
      // epoch.
      // NOTE: Only the ephemeris time really ever changes.
      aitken_basin_frame.time = moon_pfix_frame.time;

      // Adjust the time for the lookahead.
      aitken_basin_frame.time += 1.0;

   }


   // Shutdown test and resign gracefully.
   private void shutdown() {

      this.env_hla.shutdown();

      // Protect for previously called shutdown.
      if ( this.shutdown_called ) {
         return;
      }
      else {
         this.shutdown_called = true;
      }

      return;

   }


   public void print_frames()
   {

      System.out.println();
      System.out.print( "Frame timestamp: " + this.moon_inrtl_frame.time );
      System.out.println();
      this.moon_inrtl_frame.print();
      System.out.println();
      this.moon_pfix_frame.print();
      System.out.println();
      this.aitken_basin_frame.print();
      System.out.println();

      return;

   }

} // End of SISO Smackdown AitkenBasin example class.

