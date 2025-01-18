const sonic = document.getElementById('sonic');
const monster = document.getElementById('monster');
const scoreSpan = document.getElementById('scoreSpan');
const gameOver = document.getElementById('gameOver');

let score = 0;
let isJumping = false;
let isGameOver = false;

// Jump function
function jump() {
    if (!isJumping && !isGameOver) {
        isJumping = true;
        sonic.classList.add('jump');

        setTimeout(() => {
            sonic.classList.remove('jump');
            isJumping = false;
        }, 500);
    }
}

// Restart game function
function restartGame() {
    if (isGameOver) {
        isGameOver = false;
        score = 0;
        scoreSpan.textContent = score;
        gameOver.classList.add('hidden');
        monster.style.animation = 'monsterMove 2s infinite linear';
    }
}

// Check for collision and update score
setInterval(() => {
    if (!isGameOver) {
        const sonicBottom = parseInt(window.getComputedStyle(sonic).getPropertyValue('bottom'));
        const monsterRight = parseInt(window.getComputedStyle(monster).getPropertyValue('right'));
        
        // Collision detection
        if (monsterRight > 700 && monsterRight < 760 && sonicBottom <= 110) {
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden');
        }
        
        // Increase score
        if (monsterRight > 780) {
            score++;
            scoreSpan.textContent = score;
        }
    }
}, 10);

// Event listeners
document.addEventListener('keydown', (event) => {
    if (event.code === 'Space') {
        if (!isGameOver) {
            jump();
        } else {
            restartGame();
        }
        event.preventDefault();
    }
}); 