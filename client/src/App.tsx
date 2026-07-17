import './App.css';

import { HashRouter as Router, Routes, Route } from "react-router-dom";
import Board from "./board/Board";
import CreateJoin from "./lobby/CreateJoin";
import "./ui/registerEffects";

/**
 * Robot Rally Racer, same-origin Vercel build. The original Java-gateway auth
 * and lobby scenes are replaced by a single create/join screen (CreateJoin);
 * the game view (Board) is reused unchanged in spirit. HashRouter keeps routing
 * client-side so the static bundle works inside the arcade iframe subfolder.
 *
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author Patrick Røbel
 * @author Lizette Bloch Dahl Nikolajsen
 */
function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<CreateJoin />} />
        <Route path="/boardScene" element={<Board />} />
      </Routes>
    </Router>
  );
}

export default App;
