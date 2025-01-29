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

// Modify the score checking section to remove spiky balls logic
setInterval(() => {
    if (!isGameOver) {
        const sonicRect = sonic.getBoundingClientRect();
        const monsterRect = monster.getBoundingClientRect();
        
        // Collision detection logic
        if (sonicRect.right > monsterRect.left && 
            sonicRect.left < monsterRect.right && 
            sonicRect.bottom > monsterRect.top && 
            sonicRect.top < monsterRect.bottom) {
            
            // Check if the monster has both eyes
            const monsterSvg = document.querySelector('.monster-svg');
            const leftEyeVisible = monsterSvg.querySelector('.left-eye').style.display !== 'none';
            const rightEyeVisible = monsterSvg.querySelector('.right-eye').style.display !== 'none';

            if (leftEyeVisible && rightEyeVisible) {
                // Increase score by 20 if the monster has both eyes
                score += 20;
                scoreSpan.textContent = score;
            } else {
                // End the game if the monster does not have both eyes
                isGameOver = true;
                monster.style.animation = 'none';
                gameOver.classList.remove('hidden'); // Show game over message
            }
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
        monster.style.animation = 'monsterMove 1.5s infinite linear'; // Ensure constant speed
    });
});

function randomizeMonsterEyes() {
    const monsterSvg = document.querySelector('.monster-svg');
    const leftEye = monsterSvg.querySelector('.left-eye');
    const rightEye = monsterSvg.querySelector('.right-eye');
    const leftPupil = monsterSvg.querySelector('.left-pupil');
    const rightPupil = monsterSvg.querySelector('.right-pupil');

    // Reset eyes to visible
    leftEye.style.display = '';
    leftPupil.style.display = '';
    rightEye.style.display = '';
    rightPupil.style.display = '';

    // Randomly decide the eye configuration
    const randomValue = Math.random();

    if (randomValue < 0.33) {
        // Remove left eye
        leftEye.style.display = 'none';
        leftPupil.style.display = 'none';
    } else if (randomValue < 0.66) {
        // Remove right eye
        rightEye.style.display = 'none';
        rightPupil.style.display = 'none';
    }
}

// Ensure this function is called when a new monster is created
function createNewMonster() {
    // Logic to create a new monster
    randomizeMonsterEyes(); // Randomize eyes for the new monster
}

// Call createNewMonster when needed
createNewMonster(); 