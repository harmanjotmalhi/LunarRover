/**
 * Moon environment model.
 *
 * Provides the Moon environment model based on the Java Astrodynamics
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
import jat.coreNOSA.math.MatrixVector.data.VectorN;

// JAT Moon model.
import jat.coreNOSA.spacetime.Time;
import jat.coreNOSA.ephemeris.DE405;
import jat.coreNOSA.ephemeris.DE405_Body;

public class Moon extends Planet {


   public Moon()
   {
      super( "Moon", DE405_Body.MOON );
      inertial.parent_frame_name = "EarthMoonBarycentricInertial";
   }


   public void update(
      ReferenceFrame em_bary_frame,
      Time           ephem_time,
      DE405          ephemeris      )
   {

      VectorN m_pos_vel; // Position and velocity vector of Moon.
      VectorN m_lib;     // Moon libration angles.

      // Get the JAT Moon position and velocity.
      // NOTE: This will be in kilometers in the Solar System Barycenter frame.
      // NOTE: LCI for Smackdown is in meters in the EM Barycentric frame.
      m_pos_vel = ephemeris.get_planet_posvel( DE405_Body.MOON, ephem_time );

      // Compute the Moon position and velocity wrt EM Barycenter.
      inertial.position[0] = (m_pos_vel.x[0]*KM2M)-em_bary_frame.position[0];
      inertial.position[1] = (m_pos_vel.x[1]*KM2M)-em_bary_frame.position[1];
      inertial.position[2] = (m_pos_vel.x[2]*KM2M)-em_bary_frame.position[2];
      inertial.velocity[0] = (m_pos_vel.x[3]*KM2M)-em_bary_frame.velocity[0];
      inertial.velocity[1] = (m_pos_vel.x[4]*KM2M)-em_bary_frame.velocity[1];
      inertial.velocity[2] = (m_pos_vel.x[5]*KM2M)-em_bary_frame.velocity[2];

      // By definition, inertial frames are aligned.
      Matrix3x3.identity( inertial.T_parent_body );
      inertial.attitude.make_identity();
      inertial.attitude_rate[0] = 0.0;
      inertial.attitude_rate[1] = 0.0;
      inertial.attitude_rate[2] = 0.0;

      // Set the time.
      inertial.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

      // Compute the Moon's planet fixed position.
      // NOTE: This is always zero since this frame is colocated with LCI.
      fixed.position[0] = 0.0;
      fixed.position[1] = 0.0;
      fixed.position[2] = 0.0;
      // NOTE: Velocity relative to LCI is always zero.
      fixed.velocity[0] = 0.0;
      fixed.velocity[1] = 0.0;
      fixed.velocity[2] = 0.0;

      // Get the lunar libration angles and rates from the ephemeris.
      m_lib = ephemeris.get_planet_posvel( DE405_Body.MOON_LIB, ephem_time );

      // Declare some working variables.
      double phi      = m_lib.get(0);
      double theta    = m_lib.get(1);
      double psi      = m_lib.get(2);
      double phidot   = m_lib.get(3);
      double thetadot = m_lib.get(4);
      double psidot   = m_lib.get(5);

      // Declare and compute select trigonometric values.
      double cosphi   = Math.cos( phi );
      double sinphi   = Math.sin( phi );
      double costheta = Math.cos( theta );
      double sintheta = Math.sin( theta );
      double cospsi   = Math.cos( psi );
      double sinpsi   = Math.sin( psi );

      // Compute the Moon's planet fixed attitude.

      // First the transformation matirx from intertial to body.
      fixed.T_parent_body[0][0] =  cospsi*cosphi - sinpsi*costheta*sinphi;
      fixed.T_parent_body[0][1] =  cospsi*sinphi + sinpsi*costheta*cosphi;
      fixed.T_parent_body[0][2] =                  sinpsi*sintheta;

      fixed.T_parent_body[1][0] = -sinpsi*cosphi - cospsi*costheta*sinphi;
      fixed.T_parent_body[1][1] = -sinpsi*sinphi + cospsi*costheta*cosphi;
      fixed.T_parent_body[1][2] =                  cospsi*sintheta;

      fixed.T_parent_body[2][0] =  sintheta*sinphi;
      fixed.T_parent_body[2][1] = -sintheta*cosphi;
      fixed.T_parent_body[2][2] =  costheta;

      // Then compute the corresponding attitude quaternion.
      fixed.attitude.left_quat_from_transform( fixed.T_parent_body );

      // Compute the Moon's rotation vector.
      fixed.attitude_rate[0] = phidot*sintheta*sinpsi + thetadot*cospsi;
      fixed.attitude_rate[1] = phidot*sintheta*cospsi - thetadot*sinpsi;
      fixed.attitude_rate[2] = phidot*costheta        + psidot;

      // Set the time.
      fixed.time = ephem_time.mjd_tt() - MJD_TJD_OFFSET;

      return;

   }

}
