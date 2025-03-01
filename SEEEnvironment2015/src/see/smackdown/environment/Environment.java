/**
 * Environment Federate to SEE Smackdown.
 *
 * This application is a High Level Architecture (HLA) federate that provides
 * the principal celestial reference frames for the SEE Smackdown execution.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.1
 */
package see.smackdown.environment;

// Java signal handling classes.
import sun.misc.Signal;
import sun.misc.SignalHandler;

// Java utility classes.
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.prefs.Preferences;

// JArgs classes.
import jargs.gnu.CmdLineParser;

// JAT utility classes.
import jat.core.util.PathUtil;

// JAT Time classes.
import jat.coreNOSA.cm.cm;
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.spacetime.CalDate;
import jat.coreNOSA.spacetime.TimeUtils;

// JAT Ephemeris classes.
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;


class Environment {

   // Executive loop control variables.
   boolean     continue_execution = true;  // Execution loop control flag.
   long        exec_loop_counter  = 0;     // Cumulative loop counter.
   static long end_count = Long.MAX_VALUE; // Simulation termination count.

   // Declare the time related parameters.
   static final long MJD_TJD_OFFSET = 40000;   // Offset between mjd and tdt.
   static final long USEC_PER_CYCLE = 1000000; // Microseconds per exec cycle.
   static String date_format; // Format used to parse input starting date.
   static double tjd_epoch;   // Truncated Julian date start time.
   static double mjd_epoch;   // Modified Julian date start time.
   CalDate       sim_epoch;   // Start time for simulation (simulation epoch).
   CalDate       sim_date;    // Calendar date for simulation time.
   Time          sim_time;    // Simulation time object.
   double        end_time;    // End time for simulation.

   // Shutdown protection flag.
   private boolean shutdown_called = false;

   // Declare the Ephemeris related parameters.
   private String DE405_data_dir;
   private DE405  ephemeris;

   // Declare the needed environmental elements.
   SolarSystemBarycenter solar_system_barycenter;
   EarthMoonSystem       earth_moon_system;
   Planet                mars;

   // Declare the Environment federate ambassador.
   private EnvironmentHLA env_hla;
   private static String  crc_host;
   private static int     crc_port;
   private static String  federate_name;

   // Verbose output control flag.
   private static boolean verbose_output = true;
   private static boolean enable_hla = true;


   // Declare the "Main" routine for the application.
   public static void main( String[] args )
   {

      // Print out a start up message.
      System.out.println();
      System.out.println();
      System.out.println("*** SEE Smackdown Environment Federate ***");
      System.out.println();

      // Get the preferences node for this application class.
      process_preferences();

      // Parse the command line options; these will override preferences.
      process_command_line_options( args );

      // Instantiate an environment (epoch dates are in UTC).
      final Environment env = new Environment( mjd_epoch );

      // NOTE: Need to add signal handling for Ctrl-C shutdown.
      // For now, we'll just use a ShutdownHook routine.
      /*
      Runtime.getRuntime().addShutdownHook(new Thread()
      {
         @Override
         public void run()
         {
            System.out.println("Cleaning up with ShutdownHook routine!");
            env.continue_execution = false;
            env.shutdown();
         }
      });
      */
      // Register an signal handler for Ctrl-C.
      SignalHandler handler = new SignalHandler () {
         public void handle(Signal sig) {
            System.out.println("In signal handler.");
            env.continue_execution = false;
         }
      };
      Signal.handle(new Signal("INT"), handler);

      // Configure the simulation.
      env.configure();

      // Initialize the simulation.
      if ( env.initialize() == 0 ) {

         // Print out connection information if HLA is enabled.
         if ( enable_hla ) {
            System.out.println( "************************************************" );
            System.out.println( "CRC host: " + crc_host );
            System.out.println( "CRC port: " + crc_port );
         }

         // Print out initial frame values.
         if ( Environment.verbose_output ) {
            System.out.print( "   Federate Physical Time TT  (TJD): " );
            System.out.println( env.sim_time.mjd_tt() - MJD_TJD_OFFSET );
            env.print_frames();
         }

         // If we're at the run loop then go ahead and save the preferences.
         store_preferences();

         // Execute the run loop.
         env.run_loop();

      }

      // Execution is finished so shutdown.
      env.shutdown();

   }
   
   
   // Process the preferences.
   private static void process_preferences()
   {
      // Instantiate the application preferences.
      Preferences prefs = Preferences.userNodeForPackage( Environment.class );

      // Check for federate name preference.
      federate_name = prefs.get( "federate_name",
                                 "SEE Smackdown Environment" );

      // Check for date parse format preference.
      date_format = prefs.get( "date_format", "MM/dd/yyyy HH:mm:ss zzz" );

      // Check for start date (epoch) preference.
      // The default date is "4/12/2015 20:00:00 GMT" or TJD=17124.83333333334
      tjd_epoch = prefs.getDouble( "TJD_epoch", 17124.83333333349 );
      mjd_epoch = tjd_epoch + MJD_TJD_OFFSET;

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
      Preferences prefs = Preferences.userNodeForPackage( Environment.class );

      // Save the federate name preference.
      prefs.put( "federate_name", federate_name );

      // Save the CRC host location preference.
      prefs.put( "date_format", date_format );

      // Save the starting date (epoch).
      prefs.putDouble( "TJD_epoch", tjd_epoch );

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
      EnvOptionsParser options = new EnvOptionsParser();

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
      Boolean h_opt = (Boolean)options.getOptionValue( EnvOptionsParser.HELP );
      if ( h_opt != null ) {
         if ( h_opt ) {
            print_usage();
            System.exit(0);
         }
      }

      // Check for verbose output option.
      Boolean v_opt = (Boolean)options.getOptionValue( EnvOptionsParser.VERBOSE );
      if ( v_opt != null ) {
         if ( v_opt ) {
            verbose_output = true;
         }
      }

      // Check for option to disable HLA connectivity.
      Boolean hla_opt = (Boolean)options.getOptionValue( EnvOptionsParser.DISABLE_HLA );
      if ( hla_opt != null ) {
         if ( hla_opt ) {
            enable_hla = false;
         }
      }

      // Check for override of federate name.
      String n_opt = (String)options.getOptionValue( EnvOptionsParser.FED_NAME );
      if ( n_opt != null ) {
         federate_name = new String( n_opt );
      }

      // Check for override of CRC host location.
      String host_opt = (String)options.getOptionValue( EnvOptionsParser.CRC_HOST );
      if ( host_opt != null ) {
         crc_host = new String( host_opt );
      }

      // Check for override of CRC port.
      Integer port_opt = (Integer)options.getOptionValue( EnvOptionsParser.CRC_PORT );
      if ( port_opt != null ) {
         crc_port = port_opt;
      }

      // Check for the date options.
      String d_opt   = (String)options.getOptionValue( EnvOptionsParser.DATE );
      Double jd_opt  = (Double)options.getOptionValue( EnvOptionsParser.JULIAN_DATE );
      Double mjd_opt = (Double)options.getOptionValue( EnvOptionsParser.MJD );
      Double tjd_opt = (Double)options.getOptionValue( EnvOptionsParser.TJD );

      // Process the date options according to precedence.
      if ( tjd_opt != null ) {
         tjd_epoch = tjd_opt;
         mjd_epoch = tjd_epoch + MJD_TJD_OFFSET;
      }
      else if ( mjd_opt != null ) {
         tjd_epoch = mjd_opt - MJD_TJD_OFFSET;
         mjd_epoch = mjd_opt;
      }
      else if ( jd_opt != null ) {
         mjd_epoch = TimeUtils.JDtoMJD( jd_opt );
         tjd_epoch = mjd_epoch - MJD_TJD_OFFSET;
      }
      else if ( d_opt != null ) {
         SimpleDateFormat sdf = new SimpleDateFormat( date_format );

         // Parse the date string to set the simulation start date (epoch).
         try {
            sdf.parse( d_opt );
         } catch ( ParseException e ) {
            System.err.println(e.getMessage());
            print_usage();
            System.exit(2);
         }

         // Get the calendar associated with this date format.
         Calendar epoch_calendar = sdf.getCalendar();

         // Set the simulation starting epoch as modified Julian date.
         mjd_epoch = TimeUtils.JDtoMJD( cm.juliandate( epoch_calendar ) );
         tjd_epoch = mjd_epoch - MJD_TJD_OFFSET;
      }

      // Check for setting simulation run time.
      Integer run_time_opt = (Integer)options.getOptionValue( EnvOptionsParser.RUN_TIME );
      if ( run_time_opt != null ) {
         end_count = (long)((run_time_opt * 1000000.0)/(double)USEC_PER_CYCLE);
      }

      return;
   }


   private static class EnvOptionsParser extends CmdLineParser {

      // Set help message option.
      public static final Option HELP = new
         CmdLineParser.Option.BooleanOption( 'h', "help" );

      // Set verbose output.
      public static final Option VERBOSE = new
         CmdLineParser.Option.BooleanOption( 'v', "verbose" );

      // Set the starting execution date UTC calendar format.
      public static final Option DATE = new
         CmdLineParser.Option.StringOption( 'd', "date" );

      // Set the starting execution date in UTC Julian date format.
      public static final Option JULIAN_DATE = new
         CmdLineParser.Option.DoubleOption( 'j', "JD" );

      // Set the starting execution date in UTC modified Julian date format.
      public static final Option MJD = new
         CmdLineParser.Option.DoubleOption( 'm', "MJD" );

      // Set the starting execution date in UTC truncated Julian date format.
      public static final Option TJD = new
         CmdLineParser.Option.DoubleOption( 't', "TJD" );

      // Set the run time in for the simualtion in integer seconds.
      public static final Option RUN_TIME = new
         CmdLineParser.Option.IntegerOption( 'r', "run_time" );

      // Set the flag to disable HLA communications.
      public static final Option DISABLE_HLA = new
         CmdLineParser.Option.BooleanOption( 'f', "hla" );

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
      public EnvOptionsParser() {
         super();
         addOption( HELP );
         addOption( VERBOSE );
         addOption( DATE );
         addOption( JULIAN_DATE );
         addOption( MJD );
         addOption( TJD );
         addOption( RUN_TIME );
         addOption( DISABLE_HLA );
         addOption( FED_NAME );
         addOption( CRC_HOST );
         addOption( CRC_PORT );
      }
   }


   // Usage message for command line argument errors.
   private static void print_usage() {
       System.err.println( "usage: Environment [{-h,--help}]" );
       System.err.println( "                   [{-v,--verbose}]" );
       System.err.print(   "                   [{-d,--date} <" );
       System.err.println( date_format + ">]" );
       System.err.println( "                   [{-j,--JD} UTC_Julian_date]" );
       System.err.println( "                   [{-m,--MJD} UTC_modified_Julian_date]" );
       System.err.println( "                   [{-t,--TJD} UTC_truncated_Julian_date]" );
       System.err.println( "                   [{-r,--run_time} seconds]" );
       System.err.println( "                   [{-f,--hla}]" );
       System.err.println( "                   [{-n,--name} federate_name]" );
       System.err.println( "                   [{--crc_host} CRC_host_name]" );
       System.err.println( "                   [{--crc_port} CRC_port_number]" );
   }


   // Environment default constructor
   private Environment( )
   {
      double julian_date;
      double modified_julian_date;

      // Get the current date.
      Calendar cal_date = new GregorianCalendar();
      julian_date = cm.juliandate( cal_date );
      modified_julian_date = TimeUtils.JDtoMJD( julian_date );

      // Set the current simulation epoch.
      this.sim_epoch = new CalDate( modified_julian_date );
      this.sim_date = new CalDate( this.sim_epoch );

      // Initialize the simulation time.
      this.sim_time = new Time( this.sim_epoch );

      // Instantiate the federate ambassador.
      if ( enable_hla ) {
         env_hla = new EnvironmentHLA( federate_name, 1.0 );
      }

   }


   // Environment modified Julian data constructor
   private Environment( double modified_julian_date )
   {
      // Set the current simulation epoch.
      this.sim_epoch = new CalDate( modified_julian_date );
      this.sim_date = new CalDate( this.sim_epoch );

      // Initialize the simulation time.
      this.sim_time = new Time( this.sim_epoch );

      // Instantiate the federate ambassador.
      if ( enable_hla ) {
         env_hla = new EnvironmentHLA( federate_name, 1.0 );
      }

   }


   // Environment CalDate constructor
   private Environment( CalDate epoch )
   {

      // Set the current simulation epoch.
      this.sim_epoch = new CalDate( epoch );
      this.sim_date = new CalDate( epoch );

      // Initialize the simulation time.
      this.sim_time = new Time( epoch );

      // Instantiate the federate ambassador.
      if ( enable_hla ) {
         env_hla = new EnvironmentHLA( federate_name, 1.0 );
      }

   }


   // Environment calendar date constructor
   private Environment( int    year,
                        int    month,
                        int    day,
                        int    hour,
                        int    minute,
                        double second )
   {

      // Set the current simulation epoch.
      this.sim_epoch = new CalDate( year, month, day, hour, minute, second );
      this.sim_date = new CalDate( this.sim_epoch );

      // Initialize the simulation time.
      this.sim_time = new Time( this.sim_epoch );

      // Instantiate the federate ambassador.
      if ( enable_hla ) {
         env_hla = new EnvironmentHLA( federate_name, 1.0 );
      }

   }


   // Environment federate configuration.
   private void configure()
   {

      // Let's find the ephemeris files.
      PathUtil ephem_path = new PathUtil();
      String dir_sep = ephem_path.fs;
      this.DE405_data_dir = ephem_path.root_path + "data" + dir_sep + "core" + dir_sep + "ephemeris" + dir_sep + "DE405data" + dir_sep;

      System.out.println( "Ephemeris file located in:" );
      System.out.println( "   " + DE405_data_dir );
      System.out.println();

      // Specify the FOM location.
      if ( enable_hla ) {
         env_hla.configure( crc_host, crc_port );
      }

      return;

   }


   // Environment federate initialization.
   private int initialize()
   {

      // Initialize the HLA federation.
      if ( enable_hla ) {
         if ( env_hla.initialize( true, true ) != 0 ) {
            // Print out connection information.
            System.out.println( "************************************************" );
            System.out.println( "CRC host: " + crc_host );
            System.out.println( "CRC port: " + crc_port );
            System.out.println( "************************************************" );
            return 1;
         }
      }

      // Read in the Ephemeris data.
      this.ephemeris = new DE405( DE405_data_dir );

      // Initialize the reference frames.
      this.initialize_environment();

      // Update the federation reference frames to the current epoch.
      this.solar_system_barycenter.update( this.sim_time, this.ephemeris );
      this.earth_moon_system.update( this.sim_time, this.ephemeris );
      this.mars.update( this.sim_time, this.ephemeris );

      // Publish the environment federate reference frames.
      if ( enable_hla ) {
         publish_frames();
      }

      return 0;

   }


   // Environment federate executive run loop.
   private void run_loop()
   {
      double sim_exec_time;

      // Compute the simulation termination time for informational purposes.
      end_time = (end_count * USEC_PER_CYCLE) / 1000000.0;

      // Print out the starting simulation epoch.
      System.out.println( "************************************************" );
      System.out.print("Simulation Epoch: ");
      System.out.println( this.sim_epoch.toString() );
      
      // Print out the Julian date and the modified Julian date.
      System.out.print("Julian date: ");
      System.out.println( this.sim_time.jd_utc());
      System.out.print("Truncated Julian date: ");
      System.out.println( this.sim_time.mjd_utc() - MJD_TJD_OFFSET );
      System.out.println( "************************************************" );
      System.out.println();

      // This is the run loop
      while ( continue_execution ) {

         // Simple sleep based time regulation.
         try {
            Thread.sleep( 1000 );
         } catch( Exception e ) {
            System.out.print( "SEE Smackdown Environment Federate: " );
            System.out.println( "Unknown error in sleep:" );
            System.out.println( e.getMessage() );
         }

         // Wait for the time advance grant.
         if ( enable_hla ) {
            while( env_hla.wait_for_time_advance() ){
               Thread.yield();
            }
         }

         // Compute the current simulation execution time.
         sim_exec_time = (exec_loop_counter * USEC_PER_CYCLE) / 1000000.0;

         // Move clock to current simulation time.
         this.sim_time.update( sim_exec_time );

         // Do things in this section of code.
         // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

         // Update the federation reference frames.
         this.update_environment();

         // Need to publish the frames to the federation.
         if ( enable_hla ) { this.env_hla.update_published_frames(); }

         // Print out execution loop timing.
         if ( verbose_output ) {
            System.out.println();
            System.out.println( "Simulation execution loop:" );
            System.out.print( "   Executive Loop Counter: " );
            System.out.println( exec_loop_counter );
            System.out.print( "   Simulation Execution Time: " );
            System.out.println( this.sim_time.get_sim_time() );
            System.out.print( "   Federate Physical Time TAI (TJD): " );
            System.out.println( (this.sim_time.mjd_tt() - MJD_TJD_OFFSET) - (32.184/86400) );
            System.out.print( "   Federate Physical Time TT  (TJD): " );
            System.out.println( this.sim_time.mjd_tt() - MJD_TJD_OFFSET );
            System.out.print( "   Federate Physical Time UTC (TJD): " );
            System.out.println( this.sim_time.mjd_utc() - MJD_TJD_OFFSET );
            System.out.print( "   Federate Physical Time (s): " );
            System.out.format( "%.3f%n", this.sim_time.secOfDay() );
            if ( enable_hla ) {
               System.out.print( "   Federateration Execution Time (s): " );
               System.out.println( env_hla.get_hla_time() );
            }
            this.print_frames();
         }
         else {
            System.out.print( "Executive Loop Counter: " );
            System.out.print( exec_loop_counter );
            System.out.print( '\r' );
         }

         // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         // End of section to do things.

         // Request a time advance by the logical time interval.
         if ( enable_hla ) {
            env_hla.request_time_advance();
         }

         // Increment the executive loop counter.
         exec_loop_counter++;

         // Check for termination time.
         if ( exec_loop_counter > end_count ){ continue_execution = false; }

      }

      // End of run loop.  Return to calling routine.
      return;

   }


   // Shutdown Environment federate and resign gracefully.
   private void shutdown()
   {

      // Shutdown HLA.
      if ( enable_hla ) {
         env_hla.shutdown();
      }

      // Protect for previously called shutdown.
      if ( this.shutdown_called ) {
         return;
      }
      else {
         this.shutdown_called = true;
      }

      // Convert final simulation time to date format for printing.
      // NOTE: There is a loss of acuracy do to the conversion back from MJD.
      // Unfortunately, we don't quite get the same seconds as we started with.
      this.sim_date = new CalDate( this.sim_time.mjd_utc() );

      // Print out the federate execution time.
      System.out.println();
      System.out.println( "************************************************" );
      System.out.print("Environment federate shutdown times (s): ");
      System.out.println( this.sim_time.get_sim_time() );
      System.out.print("   Calendar date: ");
      System.out.println( this.sim_date.toString() );

      // Print out the calendar, Julian date and the modified Julian date.
      System.out.print("   Julian date: ");
      System.out.println( this.sim_time.jd_utc());
      System.out.print("   Truncated Julian date: ");
      System.out.println( this.sim_time.mjd_utc() - MJD_TJD_OFFSET );
      System.out.println( "************************************************" );
      System.out.println();

      return;

   }


   // Initialize the Sun-Earth-Moon environment.
   private void initialize_environment()
   {
      // Instantiate the top level solar system barycenter frame.
      solar_system_barycenter = new SolarSystemBarycenter();

      // Instantiate the Earth-Moon system and associated frames.
      earth_moon_system = new EarthMoonSystem( this.sim_time );

      // Initialize the Earth-Moon system.
      earth_moon_system.initialize( this.sim_time, this.ephemeris );

      // Instantiate Mars and associated frames.
      mars = new Planet( solar_system_barycenter.inertial,
                         "Mars", DE405_Body.MARS );

      return;

   }


   // Register the published reference frame for this federate.
   private void publish_frames()
   {

      // Register the frames to publish.
      env_hla.add_published_frame( solar_system_barycenter.sun.inertial );
      env_hla.add_published_frame( earth_moon_system.barycenter.inertial );
      env_hla.add_published_frame( earth_moon_system.barycenter.rotating );
      env_hla.add_published_frame( earth_moon_system.earth.inertial );
      env_hla.add_published_frame( earth_moon_system.earth.fixed );
      env_hla.add_published_frame( earth_moon_system.moon.inertial );
      env_hla.add_published_frame( earth_moon_system.moon.fixed );
      env_hla.add_published_frame( earth_moon_system.l2_frame );
      env_hla.add_published_frame( mars.inertial );
      env_hla.add_published_frame( mars.fixed );

      return;

   }


   // Update all the federation environmental reference frames.
   private void update_environment()
   {

      // Update the federation refernce frames to the current epoch.
      // NOTE: Update order is important: EMBary must preceed Earth & Moon.
      solar_system_barycenter.update( this.sim_time, this.ephemeris );
      earth_moon_system.update( this.sim_time, this.ephemeris );
      mars.update( this.sim_time, this.ephemeris );

   }


   public void print_frames()
   {

      System.out.println();
      System.out.print( "Frame timestamp: " );
      System.out.print( this.solar_system_barycenter.inertial.time );
      System.out.println();
      this.solar_system_barycenter.sun.inertial.print();
      System.out.println();
      this.earth_moon_system.barycenter.inertial.print();
      System.out.println();
      this.earth_moon_system.barycenter.rotating.print();
      System.out.println();
      this.earth_moon_system.earth.inertial.print();
      System.out.println();
      this.earth_moon_system.earth.fixed.print();
      System.out.println();
      this.earth_moon_system.moon.inertial.print();
      System.out.println();
      this.earth_moon_system.moon.fixed.print();
      System.out.println();
      this.earth_moon_system.l2_frame.print();
      System.out.println();
      this.mars.inertial.print();
      System.out.println();

      return;

   }

} // End of SEE Smackdown Environment class.

