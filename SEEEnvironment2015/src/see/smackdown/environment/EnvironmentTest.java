/**
 * Environment Test Federate.
 *
 * This application is a High Level Architecture (HLA) federate used to test
 * the Environment federate.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// Smackdown utility classes.
import see.smackdown.reference_frame.*;

// Java utility classes.
import java.util.prefs.Preferences;

// JArgs classes.
import jargs.gnu.CmdLineParser;


class EnvironmentTest {

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

   // Declare the Environment federate ambassador.
   private EnvironmentHLA env_hla;
   private static String  crc_host;
   private static int     crc_port;
   private static String  federate_name;

   // Verbose output control flag.
   private static boolean verbose_output = true;

   // Declare the "Main" routine for the application.
   public static void main( String[] args )
   {

      // Print out a start up message.
      System.out.println();
      System.out.println("*** Java Environment Test Federate ***");
      System.out.println();

	  // Get the preferences node for this application class.
	  EnvironmentTest.process_preferences();

	  // Parse the command line options; these will override preferences.
	  EnvironmentTest.process_command_line_options( args );

      // Instantiate a EnvironmentTest instance.
      EnvironmentTest my_test = new EnvironmentTest();

      // Catch any exceptions that might be generated in the execution loop.
      try {

         // Configure the EnvironmentTest
         my_test.configure();

         // Initialize the EnvironmentTest.
         if ( my_test.initialize() == 0 ) {
        	 
            // Print out connection information.
            System.out.println( "************************************************" );
            System.out.println( "CRC host: " + crc_host );
            System.out.println( "CRC port: " + crc_port );

            // If we're at the run loop then go ahead and save the preferences.
            store_preferences();

            // Execute the run loop.
            my_test.run();

         }

         // Execution is finished so shutdown.
         my_test.shutdown();

      } catch (Exception e) {

         // Bad Scoobees so try to see what happened.
         e.printStackTrace();

      }

   }
   
   // Process the preferences.
   private static void process_preferences()
   {
      // Instantiate the application preferences.
      Preferences prefs = Preferences.userNodeForPackage( EnvironmentTest.class );

      // Check for federate name preference.
      federate_name = prefs.get( "test_federate_name",
                                 "Environment Test" );

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
      Preferences prefs = Preferences.userNodeForPackage( EnvironmentTest.class );

      // Save the federate name preference.
      prefs.put( "test_federate_name", federate_name );

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
      EnvTestOptionsParser options = new EnvTestOptionsParser();

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
      Boolean h_opt = (Boolean)options.getOptionValue( EnvTestOptionsParser.HELP );
      if ( h_opt != null ) {
         if ( h_opt ) {
            print_usage();
            System.exit(0);
         }
      }

      // Check for verbose output option.
      Boolean v_opt = (Boolean)options.getOptionValue( EnvTestOptionsParser.VERBOSE );
      if ( v_opt != null ) {
         if ( v_opt ) {
            verbose_output = true;
         }
      }

      // Check for override of federate name.
      String n_opt = (String)options.getOptionValue( EnvTestOptionsParser.FED_NAME );
      if ( n_opt != null ) {
         federate_name = new String( n_opt );
      }

      // Check for override of CRC host location.
      String host_opt = (String)options.getOptionValue( EnvTestOptionsParser.CRC_HOST );
      if ( host_opt != null ) {
         crc_host = new String( host_opt );
      }

      // Check for override of CRC port.
      Integer port_opt = (Integer)options.getOptionValue( EnvTestOptionsParser.CRC_PORT );
      if ( port_opt != null ) {
         crc_port = port_opt;
      }

      // Check for setting simulation run time.
      Integer run_time_opt = (Integer)options.getOptionValue( EnvTestOptionsParser.RUN_TIME );
      if ( run_time_opt != null ) {
         end_count = (long)((run_time_opt * 1000000.0)/(double)USEC_PER_CYCLE);
      }

      return;
   }


   private static class EnvTestOptionsParser extends CmdLineParser {

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
      public EnvTestOptionsParser() {
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
       System.err.println( "usage: EnvironmentTest:" );
       System.err.println( "          [{-h,--help}]" );
       System.err.println( "          [{-v,--verbose}]" );
       System.err.println( "          [{-r,--run_time} seconds]" );
       System.err.println( "          [{-n,--name} federate_name]" );
       System.err.println( "          [{--crc_host} CRC_host_name]" );
       System.err.println( "          [{--crc_port} CRC_port_number]" );
   }


   // EnvironmentTest constructor
   private EnvironmentTest()
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
      
      // Only subscribe to RefeneceFrame attributes.
      env_hla.publish_reference_frames = false;
      env_hla.subscribe_reference_frames = true;

   }


   private int initialize()
   {

      if ( env_hla.initialize( true, false ) != 0 ) { return 1; }

      return 0;
   }

   
   // Executive run loop.
   private void run()
   {
      double sim_exec_time;

      // Wait on the arrival of the initialization data.
      env_hla.wait_on_initialization_data();

      // Compute the simulation termination time for informational purposes.
      end_time = (end_count * USEC_PER_CYCLE) / 1000000.0;
      
      // Set the physical time epoch from the SimConfig object in seconds.
      time_epoch = earth_inrtl_frame.time * 60 * 60 * 24;

      System.out.println( "************************************************" );
      System.out.print( "EnvironmentTest \"" );
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
               System.out.print( "EnvironmentTest \"" );
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
               System.out.print( "EnvironmentTest \"" );
               System.out.print( federate_name );
               System.out.println( "\": Unknown error in sleep:" );
               System.out.println( e.getMessage() );
            }
            //Thread.yield();
         }
         
         // Do things in this section of code.
         // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

         // Print out execution loop timing.
         System.out.println( "Times are:" );
         System.out.print( "   Executive Loop Counter: " );
         System.out.println( exec_loop_counter );
         System.out.print( "   Simulation Execution Time: " );
         System.out.println( sim_exec_time );
         System.out.print( "   EnvironmentTest Physical Time: " );
         System.out.println( time );

         // Print out ReferenceFrame info.
         System.out.print( "   Federateration Execution Time (s): " );
         System.out.println( env_hla.get_hla_time() );
         if ( EnvironmentTest.verbose_output ) {
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


   // Shutdown test and resign gracefully.
   private void shutdown() {

      this.env_hla.shutdown();

   }


   public void print_frames()
   {

      System.out.println();
      System.out.print( "Frame timestamp: " + this.sun_inrtl_frame.time );
      System.out.println();
      this.sun_inrtl_frame.print();
      System.out.println();
      this.em_bary_inrtl_frame.print();
      System.out.println();
      this.em_bary_rot_frame.print();
      System.out.println();
      this.earth_inrtl_frame.print();
      System.out.println();
      this.earth_pfix_frame.print();
      System.out.println();
      this.moon_inrtl_frame.print();
      System.out.println();
      this.moon_pfix_frame.print();
      System.out.println();
      this.em_L2_frame.print();
      System.out.println();
      this.mars_inrtl_frame.print();
      System.out.println();

      return;

   }

} // End of SISO Smackdown EnvironmentTest example class.

