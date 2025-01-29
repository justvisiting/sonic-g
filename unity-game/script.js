const sonic = document.getElementById('sonic');
const monster = document.getElementById('monster');
const scoreSpan = document.getElementById('scoreSpan');
const gameOver = document.getElementById('gameOver');
const characterSelect = document.getElementById('characterSelect');
const highScoreSpan = document.getElementById('highScoreSpan');
const gameArea = document.querySelector('.game-area');
const spikes = document.getElementById('spikes');

let score = 0;
let isJumping = false;
let isGameOver = false;
let characterSelectShown = false;
let highScore = localStorage.getItem('highScore') || 0;
let spikesActive = false;
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

// Function to randomly show spikes
function activateSpikes() {
    if (!isGameOver && Math.random() < 0.3) { // 30% chance to show spikes
        spikesActive = true;
        spikes.classList.add('active');
        spikes.style.left = '0';
        spikes.style.animation = 'spikesMove 2s linear';
        
        setTimeout(() => {
            spikes.classList.remove('active');
            spikes.style.animation = 'none';
            spikesActive = false;
            setTimeout(() => {
                spikes.style.left = '-60px';
            }, 100);
        }, 2000);
    }
}

// Start spike generation
setInterval(activateSpikes, 3000); // Try to generate spikes every 3 seconds

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
            
            // End game on collision
            isGameOver = true;
            monster.style.animation = 'none';
            gameOver.classList.remove('hidden');
        }
        
        // Spike collision detection
        if (spikesActive) {
            const spikesRect = spikes.getBoundingClientRect();
            if (sonicRect.right > spikesRect.left && 
                sonicRect.left < spikesRect.right && 
                sonicRect.bottom > spikesRect.top) {
                
                // End game on spike collision
                isGameOver = true;
                monster.style.animation = 'none';
                gameOver.classList.remove('hidden');
            }
        }
        
        // Increase score when passing a monster
        if (monsterRect.right < sonicRect.left && !monsterRect.scored) {
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
}, 10);

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