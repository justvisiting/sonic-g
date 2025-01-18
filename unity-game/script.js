const sonic = document.getElementById('sonic');
const obstacle = document.getElementById('obstacle');
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
        obstacle.style.animation = 'obstacleMove 2s infinite linear';
    }
}

// Check for collision and update score
setInterval(() => {
    if (!isGameOver) {
        const sonicBottom = parseInt(window.getComputedStyle(sonic).getPropertyValue('bottom'));
        const obstacleRight = parseInt(window.getComputedStyle(obstacle).getPropertyValue('right'));
        
        // Collision detection
        if (obstacleRight > 700 && obstacleRight < 760 && sonicBottom <= 110) {
            isGameOver = true;
            obstacle.style.animation = 'none';
            gameOver.classList.remove('hidden');
        }
        
        // Increase score
        if (obstacleRight > 780) {
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