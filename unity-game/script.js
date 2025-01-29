const sonic = document.getElementById('sonic');
const monster = document.getElementById('monster');
const scoreSpan = document.getElementById('scoreSpan');
const gameOver = document.getElementById('gameOver');
const characterSelect = document.getElementById('characterSelect');
const highScoreSpan = document.getElementById('highScoreSpan');
const gameArea = document.querySelector('.game-area');

let score = 0;
let isJumping = false;
let isGameOver = false;
let characterSelectShown = false;
let highScore = localStorage.getItem('highScore') || 0;
let spaceKeyPresses = 0;
let lastSpaceKeyTime = 0;
highScoreSpan.textContent = highScore;

// Jump function
function jump() {
    if (!isJumping && !isGameOver) {
        isJumping = true;
        sonic.classList.add('jump');

        setTimeout(() => {
            sonic.classList.remove('jump');
            setTimeout(() => {
                isJumping = false;
            }, 100);
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

// Add jump controls
function handleJump(event) {
    // For spacebar
    if (event.type === 'keydown' && event.code === 'Space') {
        event.preventDefault();
        
        // Check for quick space presses
        const currentTime = Date.now();
        if (currentTime - lastSpaceKeyTime < 500) { // Within 500ms
            spaceKeyPresses++;
            if (spaceKeyPresses >= 3) {
                score += 50;
                scoreSpan.textContent = score;
                spaceKeyPresses = 0; // Reset counter
                
                // Update high score if needed
                if (score > highScore) {
                    highScore = score;
                    localStorage.setItem('highScore', highScore);
                    highScoreSpan.textContent = highScore;
                }
            }
        } else {
            spaceKeyPresses = 1;
        }
        lastSpaceKeyTime = currentTime;
        
        if (isGameOver) {
            restartGame();
        } else {
            jump();
        }
    }
    // For mouse click
    else if (event.type === 'click') {
        if (event.target.closest('.character-select') || event.target.closest('.game-over')) {
            return;
        }
        if (isGameOver) {
            restartGame();
        } else {
            jump();
        }
    }
}

// Modify the score checking section
setInterval(() => {
    if (!isGameOver) {
        const sonicRect = sonic.getBoundingClientRect();
        const monsterRect = monster.getBoundingClientRect();
        
        // More precise collision detection
        const sonicLeft = sonicRect.left + 10; // Add some padding for more precise collision
        const sonicRight = sonicRect.right - 10;
        const sonicTop = sonicRect.top + 5;
        const sonicBottom = sonicRect.bottom - 5;
        
        const monsterLeft = monsterRect.left + 5;
        const monsterRight = monsterRect.right - 5;
        const monsterTop = monsterRect.top + 5;
        const monsterBottom = monsterRect.bottom - 5;
        
        // Collision detection logic with more precise boundaries
        if (sonicRight > monsterLeft && 
            sonicLeft < monsterRight && 
            sonicBottom > monsterTop && 
            sonicTop < monsterBottom) {
            
            // End game on collision
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden');
            
            // Stop Sonic's movement
            sonic.style.animation = 'none';
            sonic.classList.remove('jump');
            isJumping = false;
            
            // Prevent any further score increases
            score = Math.floor(score); // Ensure score is final
            scoreSpan.textContent = score;
            return; // Exit immediately on collision
        }
        
        // Only check for score increase if no collision
        if (!isGameOver && monsterRect.right < sonicRect.left && !monsterRect.scored) {
            score++;
            scoreSpan.textContent = score;
            monsterRect.scored = true;
            
            // Update high score
            if (score > highScore) {
                highScore = score;
                localStorage.setItem('highScore', highScore);
                highScoreSpan.textContent = highScore;
            }
            
            // Show character selection at score 50
            if (score === 50 && !characterSelectShown) {
                characterSelectShown = true;
                monster.style.animation = 'none';
                characterSelect.classList.remove('hidden');
                
                // Hide Knuckles option
                document.querySelector('.character-option[data-image="./images/knuckles.png"]').style.display = 'none';
            }
            
            // Update character based on score
            updateCharacterBasedOnScore();
        }
        
        // Reset monster when it goes off screen
        if (monsterRect.right < 0) {
            monster.style.animation = 'none';
            setTimeout(() => {
                monster.style.animation = 'monsterMove 1.5s infinite linear';
                monsterRect.scored = false;
                randomizeMonsterEyes();
            }, 0);
        }
    }
}, 10); // Check every 10ms for more responsive collision detection

// Add event listeners
document.addEventListener('keydown', handleJump);
gameArea.addEventListener('click', handleJump);

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', () => {
    // Make container clickable
    document.querySelector('.container').style.cursor = 'pointer';
});

// Optionally, we can also ignore keydown events that happen too quickly
let lastJumpTime = 0;
const JUMP_COOLDOWN = 600; // 600ms total cooldown (500ms jump + 100ms delay)

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