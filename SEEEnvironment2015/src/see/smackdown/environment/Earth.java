/**
 * Earth environment model.
 *
 * Provides the Earth environment model based on the Java Astrodynamics
 * Toolkit (JAT).
 *
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */
package see.smackdown.environment;

// Smackdown utility classes.
import see.smackdown.reference_frame.*;
import see.smackdown.utilities.*;

// JAT utility classes.
import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.VectorN;

// JAT Earth model.
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;
import jat.coreNOSA.spacetime.EarthRef;

public class Earth extends Planet {

   // Earth attitude model.
   private EarthRef earth_ref;


   // Default constructor.
   public Earth()
   {
      this( 0.0 );
   }


   // UTC time standard constructor.
   public Earth( double mjd_utc )
   {
      super( "Earth", DE405_Body.EARTH );
      inertial.parent_frame_name = "EarthMoonBarycentricInertial";
      earth_ref = new EarthRef( mjd_utc );
   }


   // JAT time object constructor.
   public Earth( Time time )
   {
      super( "Earth", DE405_Body.EARTH );
      inertial.parent_frame_name = "EarthMoonBarycentricInertial";
      earth_ref = new EarthRef( time );
   }


   public void update(
      ReferenceFrame em_bary_frame,
      Time           ephem_time,
      DE405          ephemeris   )
   {
      VectorN e_pos_vel; // Position and velocity vector of Earth.
      Matrix  eci2ecef;  // Attitude matrix for the Earth.

      // Update the JAT Earth reference.
      this.earth_ref.update( ephem_time );

      // Get the JAT Earth position and velocity.
      // NOTE: This will be in kilometers in the Solar System Barycenter frame.
      // NOTE: ECI for Smackdown is in meters in the EM Barycentric frame.
      e_pos_vel = ephemeris.get_planet_posvel( DE405_Body.EARTH,
                                               ephem_time        );

      // Compute the Earth position and velocity wrt EM Barycenter.
      inertial.position[0] = (e_pos_vel.x[0]*KM2M)-em_bary_frame.position[0];
      inertial.position[1] = (e_pos_vel.x[1]*KM2M)-em_bary_frame.position[1];
      inertial.position[2] = (e_pos_vel.x[2]*KM2M)-em_bary_frame.position[2];
      inertial.velocity[0] = (e_pos_vel.x[3]*KM2M)-em_bary_frame.velocity[0];
      inertial.velocity[1] = (e_pos_vel.x[4]*KM2M)-em_bary_frame.velocity[1];
      inertial.velocity[2] = (e_pos_vel.x[5]*KM2M)-em_bary_frame.velocity[2];

      // By definition, inertial frames are aligned.
      Matrix3x3.identity( inertial.T_parent_body );
      inertial.attitude.make_identity();
      inertial.attitude_rate[0] = 0.0;
      inertial.attitude_rate[1] = 0.0;
      inertial.attitude_rate[2] = 0.0;

      // Set the time.
      inertial.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

      // Compute the Earth's planet fixed position.
      // NOTE: This is always zero since this frame is colocated with ECI.
      fixed.position[0] = 0.0;
      fixed.position[1] = 0.0;
      fixed.position[2] = 0.0;
      // NOTE: Velocity relative to ECI is always zero.
      fixed.velocity[0] = 0.0;
      fixed.velocity[1] = 0.0;
      fixed.velocity[2] = 0.0;

      // Compute the Earth's planet fixed attitude.
      eci2ecef = this.earth_ref.ECI2ECEF();
      fixed.T_parent_body[0][0] = eci2ecef.A[0][0];
      fixed.T_parent_body[0][1] = eci2ecef.A[0][1];
      fixed.T_parent_body[0][2] = eci2ecef.A[0][2];
      fixed.T_parent_body[1][0] = eci2ecef.A[1][0];
      fixed.T_parent_body[1][1] = eci2ecef.A[1][1];
      fixed.T_parent_body[1][2] = eci2ecef.A[1][2];
      fixed.T_parent_body[2][0] = eci2ecef.A[2][0];
      fixed.T_parent_body[2][1] = eci2ecef.A[2][1];
      fixed.T_parent_body[2][2] = eci2ecef.A[2][2];
      fixed.attitude.left_quat_from_transform( fixed.T_parent_body );

      // Compute the Earth's rotation vector.
      fixed.attitude_rate[0] = 0.0;
      fixed.attitude_rate[1] = 0.0;
      fixed.attitude_rate[2] = EarthRef.omega_e;

      // Set the time.
      fixed.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

      return;

   }

}
