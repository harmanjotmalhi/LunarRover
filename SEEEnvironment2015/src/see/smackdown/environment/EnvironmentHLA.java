/**
 * Environment HLA model.
 *
 * This model provides the methods used to setup and control the interface
 * between the Environment executive and the HLA federation execution.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// SEE Smackdown utility classes.
import see.smackdown.reference_frame.*;

// Java utility classes.
import java.io.File;
import java.net.URL;

// HLA 1516e classes.
import hla.rti1516e.time.*;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;


public class EnvironmentHLA {

   // Declare public data interface.
   public boolean create_federation = true;
   public boolean fail_on_creation_error = false;
   public boolean publish_reference_frames = true;
   public boolean subscribe_reference_frames = false;

   // All instances of EnvironmentHLA have these definitions.
   //private static final String FEDERATION_NAME = "SEE Smackdown";
   private static final String FEDERATION_NAME = "SEE 2015";
   private static final String FEDERATE_TYPE   = "Environment";

   // Instance specific definitions.
   private String federate_name = "SEE SmackDown Environment";

   // Declare internal HLA housekeeping stuff.
   private String                        crc_host = "localhost";
   private int                           crc_port = 8989;
   private RTIambassador                 rti_ambassador;
   private EnvironmentFederateAmbassador env_fed_amb;
   private HLAinteger64TimeFactory       time_factory;
   private long                          lookahead_usec = 1000000;
   private HLAinteger64Interval          lookahead_interval;
   private TimeQueryReturn               starting_GALT;
   private boolean                       enable_time_constrained  = false;
   private boolean                       enable_time_regulating   = false;

   // Define the FOM modules.
   private static final File  coreFile    = new File("foms/SISO_SpaceFOM_core.xml");
   private static final File  environFile = new File("foms/SISO_SpaceFOM_environ.xml");
   private static final File  entityFile = new File("foms/SISO_SpaceFOM_entity.xml");
   private static       URL[] FOM_MODULES;


   // Environment default constructor
   public EnvironmentHLA( 
      String fed_name )
   {
      // Set the federate name.
      federate_name = new String( fed_name );

      // Instantiate the federate ambassador.
      env_fed_amb = new EnvironmentFederateAmbassador( federate_name );

   }


   // Environment lookahead time (s) constructor
   public EnvironmentHLA( 
      String fed_name,
      double lookahead_time )
   {
      // Set the federate name.
      federate_name = new String( fed_name );

      // Set the lookahead time.
      lookahead_usec = (long)(lookahead_time * 1000000.0);

      // Instantiate the federate ambassador.
      env_fed_amb = new EnvironmentFederateAmbassador( federate_name );

   }


   // Set the CRC host location.
   public void set_host(
      String host_name )
   {
      crc_host = new String( host_name );
      return;
   }


   // Get the CRC host location.
   public String get_host(){ return new String(crc_host); }


   // Set the CRC post location.
   public void set_port(
      int port )
   {
      crc_port = port;
      return;
   }


   // Get the CRC port.
   public int get_port(){ return crc_port; }


   // Get the current federation execution time in seconds.
   public double get_hla_time()
   {
      return( ((double)env_fed_amb.logical_time.getValue()) / 1000000.0 );
   }


   /**
    * Wait for time advance
    * 
    * This routine will return the wait status for federation time
    * advancement.
    * 
    * @return true is waiting on time advancement, false if ready to advance.
    */
   public boolean wait_for_time_advance()
   {
      return( !env_fed_amb.time_can_advance );
   }


   // Advance HLA time.
   public void request_time_advance()
   {
      try {
         env_fed_amb.time_can_advance = false;
         env_fed_amb.logical_time = env_fed_amb.logical_time.add( lookahead_interval );
         rti_ambassador.timeAdvanceRequest( env_fed_amb.logical_time );
      } catch (Exception e){
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Error in TAR!" );
         System.out.println( e.getMessage() );
         return;
      }
   }


   // Advance HLA federate time to greatest available time.
   private boolean advance_to_current_hla_time()
   {

      // For late joining federates, advance time to GALT.
      try {
         starting_GALT = rti_ambassador.queryGALT();
      } catch (Exception e) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Error in query for GALT!" );
         System.out.println( e.getMessage() );
         return( false );
      }
      if ( starting_GALT.timeIsValid ) {
         try {
        	env_fed_amb.logical_time = (HLAinteger64Time)starting_GALT.time;
            rti_ambassador.timeAdvanceRequest( starting_GALT.time );
         } catch (Exception e) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Error in TAR with GALT!" );
            System.out.println( e.getMessage() );
            return( true );
         }
      } else {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Cannot advance to current time!" );
         System.out.println( "   This may be the first time regulating federate!" );
         System.out.println();
         return( false );
      }

      // Wait here for time advance grant.
      while( wait_for_time_advance() ){
         Thread.yield();
      }

      return( true );
      
   }


   // Environment federate HLA configuration.
   public void configure()
   {

      // Specify the FOM location.
      try {
         FOM_MODULES = new URL[] { coreFile.toURI().toURL(),
               environFile.toURI().toURL(), 
               entityFile.toURI().toURL() };
      } catch ( Exception ignore ) {
      }
      
      return;

   }


   // Environment federate HLA configuration.
   public void configure( String host_name )
   {

      // Override the CRC host name.
      crc_host = new String( host_name );

      // Finish configuration.
      this.configure();

      return;

   }


   // Environment federate HLA configuration.
   public void configure( int port )
   {

      // Override the CRC port number.
      crc_port = port;

      // Finish configuration.
      this.configure();

      return;

   }


   // Environment federate HLA configuration.
   public void configure(
      String host_name,
      int   port       )
   {

      // Override the CRC host name.
      crc_host = new String( host_name );

      // Override the CRC port number.
      crc_port = port;

      // Finish configuration.
      this.configure();

      return;

   }

   
   // Environment federate HLA initialization.
   public int initialize(
      boolean time_constrained_flag,
      boolean time_regulating_flag   )
   {
      String rti_name;
      String rti_version;
      String hla_version;

      // Set up the RTI Ambassador.
      try {

         // Get the RTI factory.  This is used to "manufacture" RTI
         // related services used in the application.
         RtiFactory rti_factory = RtiFactoryFactory.getRtiFactory();

         // Get the RTI ambassador.
         rti_ambassador = rti_factory.getRtiAmbassador();
         
         // Get information on the HLA/RTI
         rti_name = rti_factory.rtiName();
         rti_version = rti_factory.rtiVersion();
         hla_version = rti_ambassador.getHLAversion();
         System.out.println( "************************************************" );
         System.out.println( "RTI Name: " + rti_name );
         System.out.println( "RTI Version: " + rti_version );
         System.out.println( "HLA Version: " + hla_version );

         // Get the encoder factory used to build encoders for the types.
         env_fed_amb.encoder_factory = rti_factory.getEncoderFactory();

      } catch (Exception e) {

         // Something went wrong in getting the ambassador or encoder factory.
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println("\": Unable to create RTI ambassador.");
         System.out.println( e.getMessage() );
         return 1;

      }

      // Specify the CRC host and port.
      // NOTE: This needs to be adjusted for different vendors!!!
      String local_settings_designator;
      if ( rti_name.equals( "pRTI 1516" ) ) {
         local_settings_designator = new String(
            "crcHost=" + crc_host + "\ncrcPort=" + crc_port );
      }
      else if ( rti_name.equals( "MAK RTI" ) ) {
         //local_settings_designator = new String(
         //   "(setqb RTI_tcpForwarderAddr \"" + crc_host + "\")" +
         //   "(setqb RTI_udpPort " + crc_port + ")" +
         //   "(setqb RTI_tcpPort " + crc_port + ")" );
         // I cannot seem to make this work.
         // So, for now you have to configure RTI connection in the RID file.
         local_settings_designator = new String("");
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println("\": Host and Port override not yet supported!");
         System.out.println("For now you must configure connection in the RID file.");
      }
      else {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.print("\": Unknow RTI name: ");
         System.out.println( rti_name );
         return 1;
      }

      // Connect to the RTI.
      try {
         rti_ambassador.connect( env_fed_amb,
                                 CallbackModel.HLA_IMMEDIATE,
                                 local_settings_designator    );
      } catch ( ConnectionFailed connect_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unable to connect to the RTI." );
         return 1;
      } catch ( InvalidLocalSettingsDesignator setting_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Error detected in local settings:" );
         System.out.println( local_settings_designator );
         return 1;
      } catch ( RTIinternalError rti_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Internal RTI error detected on connect:" );
         System.out.println( rti_error.getMessage() );
         return 1;
      } catch ( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
         return 1;
      }

      // Create the federation execution.
      if ( create_federation ) {
         try {
            rti_ambassador.createFederationExecution( FEDERATION_NAME,
                                                      FOM_MODULES,
                                                      "HLAinteger64Time" );
         } catch( InconsistentFDD fdd_consistency_error ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": FDD consitency error:" );
            System.out.println( fdd_consistency_error.getMessage() );
            return 1;
         } catch( ErrorReadingFDD fdd_read_error ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Could not read FDD:" );
            System.out.println( fdd_read_error.getMessage() );
            return 1;
         } catch( CouldNotOpenFDD fdd_open_error ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Could not open FDD:" );
            System.out.println( fdd_open_error.getMessage() );
            return 1;
         } catch( FederationExecutionAlreadyExists fed_exists_error ) {
            // NOTE: This isn't necessarily an error.  However, we are going to
            //       treat it as an error for now.
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Federation already exists:" );
            System.out.println( fed_exists_error.getMessage() );
            if ( fail_on_creation_error ) {
               return 1;
            }
            else if ( !recreate_federation() ) {
               return 1;
            }
         } catch( NotConnected not_connected_error ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Not connected to the RTI:" );
            System.out.println( not_connected_error.getMessage() );
            return 1;
         } catch( RTIinternalError rti_error ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Internal RTI error detected on join:" );
            System.out.println( rti_error.getMessage() );
            return 1;
         } catch( Exception e ) {
            System.out.print( "Federate \"" );
            System.out.print( federate_name );
            System.out.println( "\": Unexpected/unhandled error detected:" );
            System.out.println( e.getMessage() );
            return 1;
         }
      }

      // Join the mighty federation!
      try {
         rti_ambassador.joinFederationExecution( federate_name,
                                                 FEDERATE_TYPE,
                                                 FEDERATION_NAME,
                                                 FOM_MODULES      );
      } catch( CouldNotCreateLogicalTimeFactory time_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Logical time creation error:" );
         System.out.println( time_error.getMessage() );
         return 1;
      } catch( FederationExecutionDoesNotExist fed_exist_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.print( "\": Federation execution " );
         System.out.print( FEDERATION_NAME );
         System.out.println( "\" does not exist:" );
         System.out.println( fed_exist_error.getMessage() );
         return 1;
      } catch( InconsistentFDD fdd_consistency_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": FDD consitency error:" );
         System.out.println( fdd_consistency_error.getMessage() );
         return 1;
      } catch( ErrorReadingFDD fdd_read_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Could not read FDD:" );
         System.out.println( fdd_read_error.getMessage() );
         return 1;
      } catch( CouldNotOpenFDD fdd_open_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Could not open FDD:" );
         System.out.println( fdd_open_error.getMessage() );
         return 1;
      } catch( FederateAlreadyExecutionMember fed_exec_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Already joined to the federation:" );
         System.out.println( fed_exec_error.getMessage() );
         return 1;
      } catch( NotConnected not_connected_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Not connected to the RTI:" );
         System.out.println( not_connected_error.getMessage() );
         return 1;
      } catch( RTIinternalError rti_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Internal RTI error detected on join:" );
         System.out.println( rti_error.getMessage() );
         return 1;
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
         return 1;
      }

      // Enable asynchronous data delivery.
      try {
         rti_ambassador.enableAsynchronousDelivery( );
      } catch( RTIinternalError rti_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Internal RTI error detected:" );
         System.out.println( rti_error.getMessage() );
         return 1;
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
         return 1;
      }

      // Initialize the object types we're interested in:
      ReferenceFrameObject.initialize( rti_ambassador );

      // Publish all the RefenceFrame attributes.
      if ( publish_reference_frames ) {
         ReferenceFrameObject.publish( rti_ambassador );
      }

      // Subscribe to all the ReferenceFrame attributes.
      if ( subscribe_reference_frames ) {
         ReferenceFrameObject.subscribe( rti_ambassador );
      }

      // Set up time management.
      try {

         // Get the logical time factory.
         time_factory=(HLAinteger64TimeFactory)rti_ambassador.getTimeFactory();

         // Make the local logical time object.
         env_fed_amb.logical_time = time_factory.makeInitial();

         // Make the local logical time interval.
         lookahead_interval = time_factory.makeInterval( lookahead_usec );

         // Make this federate time constrained.
         enable_time_constrained = time_constrained_flag;
         if ( enable_time_constrained ) {

            // Enable time constraint.
            rti_ambassador.enableTimeConstrained();

            // Wait for time constraint to take affect.
            while( !env_fed_amb.is_time_constrained ){Thread.yield();}

         }
         
         // Advance time to the current federation execution time.
         advance_to_current_hla_time();

         // Make this federate time regulating.
         enable_time_regulating = time_regulating_flag;
         if ( enable_time_regulating ) {

            // Enable time regulation.
            rti_ambassador.enableTimeRegulation( lookahead_interval );

            // Wait for time regulation to take affect.
            while( !env_fed_amb.is_time_regulating ){Thread.yield();}

         }

      } catch( RTIinternalError rti_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Internal RTI error detected:" );
         System.out.println( rti_error.getMessage() );
         return 1;
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
         return 1;
      }

      // At initialization, time can advance.
      env_fed_amb.time_can_advance = true;

      return 0;

   }


   // Add a published reference frame.
   public void add_published_frame(
      ReferenceFrame ref_frame )
   {
      env_fed_amb.add_published_frame( ref_frame, rti_ambassador );
   }


   // Add a expected reference frame.
   public void add_expected_frame(
         ReferenceFrame ref_frame,
         String         name       )
   {
      env_fed_amb.add_expected_frame( ref_frame, name );
   }


   // Update all the federation environmental reference frames.
   public void update_published_frames()
   {
      HLAinteger64Time pub_time;

      // Get a look ahead time.
      try {
         pub_time = env_fed_amb.logical_time.add( lookahead_interval );
      } catch ( IllegalTimeArithmetic math_error ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Illegal arithmatic error:" );
         System.out.println( math_error.getMessage() );
         return;
      }

      // Update all the published frames.
      for ( ReferenceFrameObject frame_obj : env_fed_amb.published_frames.values() ) {
         frame_obj.updateAttributes( rti_ambassador, pub_time );
      }

      return;

   }


   // Shutdown the Environment HLA federation connection.
   public void shutdown() {

      // Disable time management.
      try {
         if ( enable_time_constrained ) {
            rti_ambassador.disableTimeConstrained();
         }
         if ( enable_time_regulating ) {
            rti_ambassador.disableTimeRegulation();
         }
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Time management shutdown error:" );
         System.out.println( e.getMessage() );
      }

      // Clean up connectivity to Federation Execution.
      try {

         // Resign from the Federation Execution.
         rti_ambassador.resignFederationExecution( ResignAction.DELETE_OBJECTS_THEN_DIVEST );

      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Resignation error:" );
         System.out.println( e.getMessage() );
      }

      try {

         // Destroy the Federation Execution.
         rti_ambassador.destroyFederationExecution( FEDERATION_NAME );

         // Disconnect from the RTI.
         rti_ambassador.disconnect();

      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Destruction error:" );
         System.out.println( e.getMessage() );
      }

      rti_ambassador = null;

      return;

   }


   public boolean wait_on_initialization_data()
   {
      // Wait for the initialization data.
      try {

         // We need to wait here until we're sure the initialization object
         // has been discovered.
         while ( env_fed_amb.wait_on_init_discovery ){Thread.yield();}

      } catch( Exception e ) {
         System.out.print( "EnvironmentTest \"" );
         System.out.print( federate_name );
         System.out.println( "\": Unexpected/unhandled error detected:" );
         System.out.println( e.getMessage() );
         return( false );
      }
      // We need to wait until we receive the initialization
      // data in the initialization object instance update.
      while( env_fed_amb.wait_on_init_data ){Thread.yield();}

      return( true );
   }


   private boolean recreate_federation()
   {
      // Destroy the Federation Execution.
      try {
         rti_ambassador.destroyFederationExecution( FEDERATION_NAME );
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Destruction error:" );
         System.out.println( e.getMessage() );
         return( false );
      }
      // Try to recreate the Federation Execution.
      try {
         rti_ambassador.createFederationExecution( FEDERATION_NAME,
                                                   FOM_MODULES,
                                                   "HLAinteger64Time" );
      } catch( Exception e ) {
         System.out.print( "Federate \"" );
         System.out.print( federate_name );
         System.out.println( "\": Recreation error:" );
         System.out.println( e.getMessage() );
         return( false );
      }
      return( true );
   }

} // End of SEE Smackdown Environment class.

