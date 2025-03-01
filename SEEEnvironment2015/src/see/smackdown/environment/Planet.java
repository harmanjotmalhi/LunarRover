/**
 * General planet environment model.
 *
 * Provides the generic planet environment model.
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// Smackdown utility classes.
import see.smackdown.reference_frame.*;
import see.smackdown.utilities.*;

// JAT utility classes.
import jat.coreNOSA.math.MatrixVector.data.VectorN;

// JAT Earth model.
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;

public class Planet {

   static final long KM2M = 1000; // Conversion from kilometers to meters.
   static final long MJD_TJD_OFFSET = 40000; // Offset between mjd and tdt.

   // Declare the planet reference frame data.
   public String         name;
   public ReferenceFrame inertial;
   public ReferenceFrame fixed;

   // Reference to parent inertial reference frame;
   private ReferenceFrame parent;

   // Planetary Ephemeris body designator,
   private DE405_Body id;

   public Planet ( String planet_name, DE405_Body planet_id )
   {
      parent   = null;
      name     = new String( planet_name );
      id       = planet_id;
      inertial = new ReferenceFrame( planet_name + "CentricInertial" );
      fixed    = new ReferenceFrame( planet_name + "CentricInertial",
                                     planet_name + "CentricFixed" );
   }

   public Planet (
      ReferenceFrame parent_ref_frame,
      String         planet_name,
      DE405_Body     planet_id         )
   {
      parent   = parent_ref_frame;
      name     = new String( planet_name );
      id       = planet_id;
      inertial = new ReferenceFrame( parent_ref_frame.ref_frame_name,
                                     planet_name + "CentricInertial" );
      fixed    = new ReferenceFrame( planet_name + "CentricInertial",
                                     planet_name + "CentricFixed" );
   }


   public void update(
      Time  ephem_time,
      DE405 ephemeris   )
   {

      VectorN e_pos_vel; // Position and velocity vector of Earth.

      // Get the JAT planet position and velocity.
      // NOTE: This will be in kilometers in the Solar System Barycenter frame.
      e_pos_vel = ephemeris.get_planet_posvel( this.id, ephem_time );

      // Compute the planet's position and velocity.
      inertial.position[0] = e_pos_vel.x[0] * KM2M;
      inertial.position[1] = e_pos_vel.x[1] * KM2M;
      inertial.position[2] = e_pos_vel.x[2] * KM2M;
      inertial.velocity[0] = e_pos_vel.x[3] * KM2M;
      inertial.velocity[1] = e_pos_vel.x[4] * KM2M;
      inertial.velocity[2] = e_pos_vel.x[5] * KM2M;

      // By definition, inertial frames are aligned.
      Matrix3x3.identity( inertial.T_parent_body );
      inertial.attitude.make_identity();
      inertial.attitude_rate[0] = 0.0;
      inertial.attitude_rate[1] = 0.0;
      inertial.attitude_rate[2] = 0.0;

      // Set the time.
      inertial.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

      // Unfortunately, only the planetary attitude of the Earth and the Moon
      // are available in the JAT DE405 implementation.  That's not unusual in
      // that most attitude models are very planet specific and not really
      // well know.
      // So, the planetary attitude is NOT updated here yet.

      // Set the time.
      fixed.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

   }

}
