import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className={styles.navbar}>
      <div className={styles.navInner}>
        <div className={styles.brand}>
          <span className={styles.logo}>🏦</span>
          <Link to="/dashboard" className={styles.brandName}>PayApp</Link>
        </div>
        <ul className={styles.navLinks}>
          <li><Link to="/dashboard">Dashboard</Link></li>
          <li><Link to="/transfer">Transfer</Link></li>
          <li><Link to="/transactions">History</Link></li>
        </ul>
        <div className={styles.userSection}>
          <button className={styles.avatarBtn} onClick={() => navigate('/profile')} title="View Profile">
            {(user?.fullName || user?.username || 'U').charAt(0).toUpperCase()}
          </button>
          <button className={styles.logoutBtn} onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
}
