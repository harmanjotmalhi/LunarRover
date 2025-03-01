/**
 * Euler Angle model.
 *
 * These are a start on some very simple Euler angle routines.
 * Euler angles are often used as a potentially singular method for
 * representing rotations or attitudes.  There are a lot more methods
 * required to use Euler angles in the propagation of the rotational state
 * of an object.  However, these will provide the basic methods for going
 * to and from a more traditional transformation matrix representation.
 *
 * @author   David Hammen
 * @author   Edwin Z. Crues <edwin.z.crues@nasa.gov>
 * @version  0.0
 */

package see.smackdown.utilities;

public class EulerAngles {
   
   public enum EulerSequence {
      
      // Traditional aerodynamic sequences named by axes
      EulerXYZ( new int[]{0, 1, 2},   0,    2,    true,   true ),
      EulerXZY( new int[]{0, 2, 1},   0,    1,   false,   true ),
      EulerYZX( new int[]{1, 2, 0},   1,    0,    true,   true ),
      EulerYXZ( new int[]{1, 0, 2},   1,    2,   false,   true ),
      EulerZXY( new int[]{2, 0, 1},   2,    1,    true,   true ),
      EulerZYX( new int[]{2, 1, 0},   2,    0,   false,   true ),

      // Astronomical Euler sequences, again named by axes
      EulerXYX( new int[]{0, 1, 0},   2,    2,    true,  false ),
      EulerXZX( new int[]{0, 2, 0},   1,    1,   false,  false ),
      EulerYZY( new int[]{1, 2, 1},   0,    0,    true,  false ),
      EulerYXY( new int[]{1, 0, 1},   2,    2,   false,  false ),
      EulerZXZ( new int[]{2, 0, 2},   1,    1,    true,  false ),
      EulerZYZ( new int[]{2, 1, 2},   0,    0,   false,  false );

      private final int [] indices = new int[3]; /* --
         The axes about which the rotations are performed in the order in which
         the rotations are performed, with X=0, Y=1, Z=2. For example, an XYZ or
         roll pitch yaw sequence is {0,1,2} while a ZXZ sequence is {2,0,2}. */

      private final int alternate_x; /* --
         The initial element of the sequence for aerodynamics sequences,
         but the index of the omitted axis for astronomical sequences.
         For example, the omitted axis in a ZXZ sequence is Y=1. */

      private final int alternate_z; /* --
         The final element of the sequence for aerodynamics sequences,
         but the index of the omitted axis for astronomical sequences. */

      private final boolean is_even_permutation; /* --
         Indicates whether the 3-axis rotation sequence generated by replacing the
         final element of the sequence with the one axis not specified by the
         first two elements of the sequence is an even (true) permutation or odd
         permutation (false) of XYZ.
         The alternative 3-axis sequence is identical to the original sequence in
         the case of aerodynamics sequences. The astronomical ZXZ sequence becomes
         ZXY via this replacement rule. Since ZXY is an even permutation of XYZ,
         the is_even_permutation member for a ZXZ sequence is true. */

      private final boolean is_aerodynamics_sequence; /* --
         True if the sequence is an aerodynamics sequence such as XYZ;
         false for an astronomical sequence such as ZXZ. */
      
      // Required constructor for an EulerSequence.
      EulerSequence( int [] indx, int x, int z, boolean even, boolean aero )
      {
         this.indices[0] = indx[0];
         this.indices[1] = indx[1];
         this.indices[2] = indx[2];
         this.alternate_x = x;
         this.alternate_z = z;
         this.is_even_permutation = even;
         this.is_aerodynamics_sequence = aero;
      }
      
   }
   
   // Euler angle sequence.
   private EulerSequence sequence;

   // The Euler angle triplet.
   private double [] angles = new double[3];
   
   private static double gimbal_lock_threshold = 1e-13; /*
     Threshold for detecting gimbal lock in compute_euler_angles_from_matrix. */

   // Construct default Euler angles.
   public EulerAngles ()
   {
      sequence  = EulerSequence.EulerXYZ;
      angles[0] = 0.0;
      angles[1] = 0.0;
      angles[2] = 0.0;
   }

   
   // Construct Euler angles from values.
   public EulerAngles (
      final EulerSequence new_sequence,
      final double []     new_angles   )
   {
      this.set_sequence( new_sequence );
      this.set_angles( new_angles );
   }

   
   // Construct Euler angles from transformation matrix.
   public EulerAngles (
      final EulerSequence new_sequence,
      final double [][]   trans_matrix  )
   {
      this.set_sequence( new_sequence );
      this.set_from_matrix( trans_matrix );
   }

   
   // Construct Euler angles from left-quaternion.
   public EulerAngles (
      final EulerSequence new_sequence,
      final Quaternion    quaternion  )
   {
      double [][] trans_matrix = new double[3][3];
      this.set_sequence( new_sequence );
      quaternion.left_quat_to_transform( trans_matrix );
      this.set_from_matrix( trans_matrix );
   }
   
   
   public EulerSequence get_sequence ( )
   {
      return( this.sequence );
   }
   
   
   public void set_sequence (
      EulerSequence new_sequence )
   {
      if( new_sequence != this.sequence ){ this.sequence = new_sequence; }
      return;
   }
   
   
   public double[] get_angles ( )
   {
      return( this.angles );
   }
   
   
   public void set_angles (
      double [] new_angles )
   {
      this.angles[0] = new_angles[0];
      this.angles[1] = new_angles[1];
      this.angles[2] = new_angles[2];
   }
   
   
   public void set_from_left_quaternion (
      final Quaternion quaternion )
   {
      double [][] trans_matrix = new double[3][3];
      quaternion.left_quat_to_transform( trans_matrix );
      this.set_from_matrix( trans_matrix );
      return;
   }
   
   
   public Quaternion get_left_quaternion ( )
   {
      Quaternion [] q    = new Quaternion[3];
      Quaternion    q21  = new Quaternion();
      Quaternion    quat = new Quaternion();

      for (int ii = 0; ii < 3; ii++) {
         double htheta = 0.5*this.angles[ii];
         double cosht = Math.cos (htheta);
         double sinht = Math.sin (htheta);
         q[ii].scalar = cosht;
         q[ii].vector[this.sequence.indices[ii]] = -sinht;
      }

      q[2].multiply (q[1], q21);
      q21.multiply (q[0], quat);
      quat.normalize ();
      
      return quat;
   }
   
   /*
   Purpose:
     (Extract an Euler sequence from the transformation matrix.
      A transformation matrix constructed from an XYZ Euler sequence
      is of the form @f[
         \left[\array{ccc}
            \cos\psi\cos\theta & \cdots & \cdots \\
           -\sin\psi\cos\theta & \cdots & \cdots \\
            \sin\theta  & -\cos\theta\sin\phi & \cos\theta\cos\phi
         \endarray\right]
      @f]
      Note that the [2][0] element of the matrix depends on theta only.
      The other two elements of the leftmost column are simple terms that depend on
      theta and psi only, and the other two elements of the bottommost row are
      simple terms that depend on theta and phi only.
      Those five elements are the key to extracting an XYZ Euler sequence from a
      transformation matrix.
      The same principle applies to all twelve of the Euler sequences:
      Five key elements contain all of the information needed to extract the
      desired sequence. The location and form of those key elements of course
      depends on the sequence.

      A problem arises in the above when cos(theta) is zero, or nearly so. This
      siutation is called 'gimbal lock'. Those four elements used to determine phi
      and psi are zero or nearly so. Fortunately That ugly stuff isn't so ugly in
      the case of gimbal lock. Once again looking at the matrix generated from an
      XYZ Euler sequence, when theta=pi/2 the matrix becomes @f[
         \left[\array{ccc}
             0 & \sin(\phi+\psi) & -cos(\phi+\psi) \\
             0 &  \cos(\phi+\psi) & \sin(\phi+\psi) \\
             1 & 0 & 0
         \endarray\right]
      @f]
      In this case there no way to determine both phi and psi; all that can be
      determined is their sum. One way to overcome this problem is to arbitrarily
      set one of those angles to an arbitrary value such as zero. That is the
      approach used in this method. This arbitrary setting enables an XYZ Euler
      sequence to be extracted from the matrix even in the case of gimbal lock.
      The same principle once again applies to all twelve sequences.

      In summary, for a transformation matrix corresponding to an XYZ sequence,
       - The [2][0] element of the matrix specifies theta.
       - The [1][0] and [0][0] elements of the matrix specify psi.
       - The [2][1] and [2][2] elements of the matrix specify phi.
         These psi and phi values are valid only when gimbal lock is not present.
       - The [1][2] and [1][1] elements of the matrix specify phi in the
         case of gimbal lock.

      Extending this analysis to the remaining eleven sequences provides the
      essential information needed to extract the desired Euler angles from a
      transformation matrix. This information is captured in the EulerInfo
      array Euler_info defined at the head of this file. With a
      reference <tt>info</tt> to the appropriate element of this array,
       - The [info.indices[2]][info.indices[0]] element of the matrix
         specifies the angle theta.
       - The [info.indices[1]][info.indices[0]] and
             [info.alternate_x][info.indices[0]] elements of the matrix
         specify the angle psi when gimbal lock is not present.
       - The [info.indices[2]][info.indices[1]] and
             [info.indices[2]][info.alternate_z] elements of the matrix
         specify the angle phi when gimbal lock is not present.
       - The [info.indices[1]][info.alternate_z] and
             [info.indices[1]][info.indices[1]] elements of the matrix
         specify angle phi when gimbal lock is present.)

   Assumptions and limitations:
     ((To within numerical accuracy, the transformation matrix in the
       Orientation object @e is a proper transformation matrix:
        - The magnitude of each row and column vector is nearly one.
        - The inner product of any two different rows / two different columns of
          the matrix nearly zero.
        - The determinant of the matrix is nearly one.
        - An element whose value is outside the range [-1,1] is only slightly
          outside that range and the deviation is numerical.))
   */
   public void set_from_matrix (
      final double [][] trans ) // In:  -- Transformation matrix
   {

      double phi;            // First Euler angle
      double theta;          // Second Euler angle
      double psi;            // Third Euler angle

      double cos_phi;        // cos(phi) * extra stuff * sign
      double sin_phi;        // sin(phi) * extra stuff * sign
      double cos_psi;        // cos(psi) * extra stuff * sign
      double sin_psi;        // sin(psi) * extra stuff * sign
      double theta_val;      // sin(theta), -sin(theta) or cos(theta)
      double alt_theta_val1; // Conceptually, 1-theta_val^2
      double alt_theta_val2; // Conceptually, 1-theta_val^2
      double alt_theta_val;  // Conceptually, 1-theta_val^2


      // Extract the key elements from the matrix assuming that this is not
      // a gimbal lock situation.
      // The trans[sequence.indices[2]][sequence.indices[0]] element is
      //  *  sin(theta) for even permutation aerodynamics sequences,
      //  * -sin(theta) for odd permutation aerodynamics sequences, or
      //  *  cos(theta) for all astronomical sequences.
      theta_val = trans[sequence.indices[2]][sequence.indices[0]];

      // Get terms containing the sines and cosines of the first and third Euler
      // angle times sin(theta) or cos(theta) (and sometimes negated).
      sin_phi = trans[sequence.indices[2]][sequence.indices[1]];
      cos_phi = trans[sequence.indices[2]][sequence.alternate_z];
      sin_psi = trans[sequence.indices[1]][sequence.indices[0]];
      cos_psi = trans[sequence.alternate_x][sequence.indices[0]];

      // Compute alternative theta values based on the above four terms.
      alt_theta_val1 = Math.sqrt (sin_phi*sin_phi + cos_phi*cos_phi);
      alt_theta_val2 = Math.sqrt (sin_psi*sin_psi + cos_psi*cos_psi);
      alt_theta_val  = 0.5 * (alt_theta_val1 + alt_theta_val2);

      // theta_val is -sin(theta) for odd permutation aerodynamics sequences.
      // Negate to get rid of the minus sign.
      if (sequence.is_aerodynamics_sequence && (! sequence.is_even_permutation)) {
         theta_val = -theta_val;
      }


      // Compute theta.
      if (alt_theta_val < Math.abs (theta_val)) {
         double alt_theta = Math.asin (alt_theta_val);

         if (sequence.is_aerodynamics_sequence) {
            if (theta_val < 0.0) {
               theta = -0.5*Math.PI + alt_theta;
            }
            else {
               theta =  0.5*Math.PI - alt_theta;
            }
         }
         else {
            if (theta_val < 0.0) {
               theta = Math.PI - alt_theta;
            }
            else {
               theta =  alt_theta;
            }
         }
      }
      else {
         if (sequence.is_aerodynamics_sequence) {
            theta = Math.asin (theta_val);
         }
         else {
            theta = Math.acos (theta_val);
         }
      }


      // Not in a gimbal lock situation:
      // Compute sin_phi, cos_phi, sin_psi, and cos_psi.
      //  - These are not the sine and cosine of the Euler angles phi and psi.
      //    Rather, they are sin(phi) etc. scaled by a common positive number.
      //  - Key elements of the matrix are of the form
      //      sign*cos(theta)*sin(phi) etc.
      //  - The trick then is to find these key elements and then ensure that
      //    sign*cos(theta) is positive for each of the four values.
      //  - The specifics of the sign correction are depend on whether the
      //    sequence is an aerodynamical or astronomical sequence.
      if (alt_theta_val > gimbal_lock_threshold) {

         // Correct signs for aerodynamic sequences.
         if (sequence.is_aerodynamics_sequence) {

            // The sine values have the wrong sign for right-handed aero sequences.
            if (sequence.is_even_permutation) {
               sin_phi = -sin_phi;
               sin_psi = -sin_psi;
            }
         }

         // Correct signs for astronomical sequences.
         else {

            // A cosine term has the wrong sign in the case of astro sequences.
            // The term with the wrong sign is cos_phi for even permutations but
            // cos_psi for odd permutations.
            if (sequence.is_even_permutation) {
               cos_phi = -cos_phi;
            }
            else {
               cos_psi = -cos_psi;
            }
         }

         // Compute phi and psi.
         phi = Math.atan2 (sin_phi, cos_phi);
         psi = Math.atan2 (sin_psi, cos_psi);
      }


      // In a gimbal lock situation:
      // All that can be determine is the difference between / sum of phi and psi.
      // Arbitrarily setting psi to zero resolves this issue.
      else {
         // Compute sin_phi and cos_phi with the same constraint on the
         // common scale factor as outlined above.
         sin_phi = trans[sequence.indices[1]][sequence.alternate_z];
         cos_phi = trans[sequence.indices[1]][sequence.indices[1]];

         // The sine value has the wrong sign for odd sequences.
         if (! sequence.is_even_permutation) {
            sin_phi = -sin_phi;
         }

         // Compute phi and psi.
         phi = Math.atan2 (sin_phi, cos_phi);
         psi = 0.0;
      }

      // Save the computed angles in the object.
      angles[0] = phi;
      angles[1] = theta;
      angles[2] = psi;

      return;
   }

   
   /*
   Purpose:
     (Compute the transformation matrix from the Euler sequence.
      The matrix is formed by generating a sequence of three simple transformation
      matrices corresponding to the three rotations. The composite transformation
      matrix is the reverse-order product of these three simple matrices.)
  */
   public double [][] get_matrix ()
   {
      double [][] trans = new double[3][3];
      
      double [][][] m = new double [3][3][3];
      double [][] m21 = new double [3][3];
      double sin_theta, cos_theta;

      for (int ii = 0; ii < 3; ii++) {
         Matrix3x3.initialize (m[ii]);
         sin_theta = Math.sin (this.angles[ii]);
         cos_theta = Math.cos (this.angles[ii]);
         switch ( this.sequence.indices[ii] ) {
         case 0:
            m[ii][0][0] = 1.0;
            m[ii][1][1] =  cos_theta;
            m[ii][1][2] =  sin_theta;
            m[ii][2][1] = -sin_theta;
            m[ii][2][2] =  cos_theta;
            break;
         case 1:
            m[ii][1][1] = 1.0;
            m[ii][0][0] =  cos_theta;
            m[ii][0][2] = -sin_theta;
            m[ii][2][0] =  sin_theta;
            m[ii][2][2] =  cos_theta;
            break;
         case 2:
            m[ii][2][2] = 1.0;
            m[ii][0][0] =  cos_theta;
            m[ii][0][1] =  sin_theta;
            m[ii][1][0] = -sin_theta;
            m[ii][1][1] =  cos_theta;
            break;
         }
      }

      Matrix3x3.product (m[2], m[1], m21);
      Matrix3x3.product (m21, m[0], trans);

      return trans;
   }
   
}
