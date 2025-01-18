const sonic = document.getElementById('sonic');
const monster = document.getElementById('monster');
const scoreSpan = document.getElementById('scoreSpan');
const gameOver = document.getElementById('gameOver');
const characterSelect = document.getElementById('characterSelect');
const highScoreSpan = document.getElementById('highScoreSpan');

let score = 0;
let isJumping = false;
let isGameOver = false;
let characterSelectShown = false;
let highScore = localStorage.getItem('highScore') || 0;
highScoreSpan.textContent = highScore;

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
        monster.style.animation = 'monsterMove 3s infinite linear';

        // Reset Sonic's position
        sonic.style.left = '50px';  // Reset to starting position
        sonic.style.bottom = '50px';  // Reset to starting height
    }
}

// Add a function to check and update the character based on score
function updateCharacterBasedOnScore() {
    const currentImage = sonic.style.backgroundImage;
    const tailsImage = "url('./images/tails.png')";
    const sonicImage = "url('./images/sonic.png')";

    if (currentImage === tailsImage && score < 50) {
        sonic.style.backgroundImage = sonicImage;
    }
}

// Check for collision and update score
setInterval(() => {
    if (!isGameOver) {
        const sonicRect = sonic.getBoundingClientRect();
        const monsterRect = monster.getBoundingClientRect();
        
        // Collision detection using rectangles
        if (sonicRect.right > monsterRect.left && 
            sonicRect.left < monsterRect.right && 
            sonicRect.bottom > monsterRect.top && 
            sonicRect.top < monsterRect.bottom) {
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden');
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