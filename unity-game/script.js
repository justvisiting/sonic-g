const sonic = document.getElementById('sonic');
const monster = document.getElementById('monster');
const scoreSpan = document.getElementById('scoreSpan');
const gameOver = document.getElementById('gameOver');
const characterSelect = document.getElementById('characterSelect');
const highScoreSpan = document.getElementById('highScoreSpan');
const eggman = document.getElementById('eggman');

let score = 0;
let isJumping = false;
let isGameOver = false;
let characterSelectShown = false;
let highScore = localStorage.getItem('highScore') || 0;
highScoreSpan.textContent = highScore;

let laserIntervalId;

// Jump function
function jump() {
    if (!isJumping && !isGameOver) {
        isJumping = true;
        sonic.classList.add('jump');

        setTimeout(() => {
            sonic.classList.remove('jump');
            // Add a small delay before allowing next jump
            setTimeout(() => {
                isJumping = false;
            }, 100); // 100ms cooldown between jumps
        }, 500);
    }
}

// Restart game function
function restartGame() {
    if (isGameOver) {
        isGameOver = false;
        score = 0;
        characterSelectShown = false;
        scoreSpan.textContent = score;
        gameOver.classList.add('hidden');
        
        // Reset Sonic's position
        sonic.style.left = '50px';  // Reset to starting position
        sonic.style.bottom = '50px';  // Reset to starting height

        // Reset monster's animation
        monster.style.animation = 'none';  // Stop the animation
        setTimeout(() => {
            monster.style.animation = 'monsterMove 1.5s infinite linear';  // Restart the animation
        }, 0);  // Use a timeout to ensure the animation restarts
    }
}

// Add a function to check and update the character based on score
function updateCharacterBasedOnScore() {
    const currentImage = sonic.style.backgroundImage;
    const tailsImage = "url('./images/tails.png')";
    const knucklesImage = "url('./images/knuckles.png')";
    const sonicImage = "url('./images/sonic.png')";

    // If score is 0, revert to Sonic
    if (score === 0 && currentImage !== sonicImage) {
        sonic.style.backgroundImage = sonicImage;
    }

    // If score is less than 50 and character is Tails, revert to Sonic
    if (currentImage === tailsImage && score < 50) {
        sonic.style.backgroundImage = sonicImage;
    }
}

// Function to shoot lasers
function shootLaser() {
    const laser = document.createElement('div');
    laser.classList.add('laser');
    laser.style.left = `${Math.random() * 800}px`; // Random horizontal position
    document.querySelector('.game-area').appendChild(laser);

    // Move the laser downwards
    const laserInterval = setInterval(() => {
        const laserRect = laser.getBoundingClientRect();
        const sonicRect = sonic.getBoundingClientRect();

        // Check for collision with Sonic or Tails
        if (laserRect.bottom > sonicRect.top && 
            laserRect.left < sonicRect.right && 
            laserRect.right > sonicRect.left) {
            clearInterval(laserInterval);
            laser.remove();
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden');
        }

        // Remove laser if it goes out of bounds
        if (laserRect.top > 300) {
            clearInterval(laserInterval);
            laser.remove();
        }
    }, 10);
}

// Modify the score checking section to include Eggman and difficulty increase
setInterval(() => {
    if (!isGameOver) {
        const sonicRect = sonic.getBoundingClientRect();
        const monsterRect = monster.getBoundingClientRect();
        
        // Original collision detection logic
        if (sonicRect.right > monsterRect.left && 
            sonicRect.left < monsterRect.right && 
            sonicRect.bottom > monsterRect.top && 
            sonicRect.top < monsterRect.bottom) {
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden'); // Show game over message
        }
        
        // Increase score and check for character selection
        if (monsterRect.right < sonicRect.left && !monsterRect.scored) {
            score++;
            scoreSpan.textContent = score;
            monsterRect.scored = true;
            
            // Update high score
            if (score > highScore) {
                highScore = score;
                highScoreSpan.textContent = highScore;
                localStorage.setItem('highScore', highScore);
            }
            
            // Show character selection at score 50
            if (score === 50 && !characterSelectShown) {
                characterSelectShown = true;
                monster.style.animation = 'none';
                characterSelect.classList.remove('hidden');
                
                // Hide Knuckles option
                document.querySelector('.character-option[data-image="./images/knuckles.png"]').style.display = 'none';
            }

            // Show Eggman and start shooting lasers at score 100
            if (score === 100) {
                eggman.classList.remove('hidden');
                laserIntervalId = setInterval(shootLaser, 2000); // Shoot a laser every 2 seconds
            }

            // Increase difficulty every 50 points after 100
            if (score > 100 && score % 50 === 0) {
                clearInterval(laserIntervalId);
                const newInterval = Math.max(500, 2000 - (score - 100) * 10); // Decrease interval to a minimum of 500ms
                laserIntervalId = setInterval(shootLaser, newInterval);
            }
        }

        // Check and update character based on score
        updateCharacterBasedOnScore();
    }
}, 10);

// Optionally, we can also ignore keydown events that happen too quickly
let lastJumpTime = 0;
const JUMP_COOLDOWN = 600; // 600ms total cooldown (500ms jump + 100ms delay)

// Event listeners
document.addEventListener('keydown', (event) => {
    if (event.code === 'Space') {
        const currentTime = Date.now();
        if (!isGameOver) {
            if (currentTime - lastJumpTime >= JUMP_COOLDOWN) {
                jump();
                lastJumpTime = currentTime;
            }
        } else {
            restartGame();
        }
        event.preventDefault();
    }
});

// Add character selection event listeners
document.querySelectorAll('.character-option').forEach(option => {
    option.addEventListener('click', () => {
        const newImageUrl = option.dataset.image;
        sonic.style.backgroundImage = `url('${newImageUrl}')`;
        characterSelect.classList.add('hidden');
        monster.style.animation = 'monsterMove 3s infinite linear';
    });
}); 