const express = require('express');
const cors = require('cors');
const app = express();
const port = 3000;

// In-memory storage for leaderboard (replace with a database in production)
let leaderboard = [];

app.use(cors());
app.use(express.json());

// Serve static files
app.use(express.static('.'));

// Get top players
app.get('/api/leaderboard', (req, res) => {
    // Sort by level in descending order
    const sortedLeaderboard = [...leaderboard].sort((a, b) => b.level - a.level);
    res.json(sortedLeaderboard.slice(0, 10)); // Return top 10
});

// Update player score
app.post('/api/leaderboard', (req, res) => {
    const { playerName, level } = req.body;
    
    if (!playerName || !level) {
        return res.status(400).json({ error: 'Player name and level are required' });
    }

    // Find existing player
    const existingPlayerIndex = leaderboard.findIndex(p => p.playerName === playerName);
    
    if (existingPlayerIndex !== -1) {
        // Update existing player if new level is higher
        if (level > leaderboard[existingPlayerIndex].level) {
            leaderboard[existingPlayerIndex].level = level;
        }
    } else {
        // Add new player
        leaderboard.push({ playerName, level });
    }

    res.json({ success: true });
});

app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});
