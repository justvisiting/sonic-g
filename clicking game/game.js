class ClickingGame {
    constructor() {
        this.multiplier = 1;
        this.clickStreak = 0;
        this.lastClickTime = Date.now();
        this.candyCount = parseInt(localStorage.getItem('candyCount')) || 0;
        this.level = parseInt(localStorage.getItem('level')) || 1;
        this.candiesForNextLevel = this.calculateRequiredCandies();

        // DOM elements
        this.target = document.getElementById('target');
        this.multiplierElement = document.getElementById('multiplier');
        this.candyCountElement = document.getElementById('candyCount');
        this.levelElement = document.getElementById('level');
        this.progressTextElement = document.getElementById('progressText');
        this.progressFillElement = document.getElementById('progressFill');

        // Initialize
        this.updateDisplay();
        this.setupEventListeners();
    }

    calculateRequiredCandies() {
        return this.level * 10;
    }

    setupEventListeners() {
        this.target.addEventListener('click', () => this.handleClick());
    }

    handleClick() {
        const currentTime = Date.now();
        const timeDiff = currentTime - this.lastClickTime;

        // Update streak and multiplier based on click speed
        if (timeDiff < 500) {
            this.clickStreak++;
            if (this.clickStreak >= 10) {
                this.multiplier = 3;
            } else if (this.clickStreak >= 5) {
                this.multiplier = 2;
            }
        } else {
            this.clickStreak = 0;
            this.multiplier = 1;
        }
        
        // Add candy canes based on multiplier
        const candiesEarned = 1 * this.multiplier;
        this.candyCount += candiesEarned;
        localStorage.setItem('candyCount', this.candyCount);

        // Check for level up
        if (this.candyCount >= this.candiesForNextLevel) {
            this.levelUp();
        }

        this.lastClickTime = currentTime;
        this.updateDisplay();
    }

    levelUp() {
        this.level++;
        localStorage.setItem('level', this.level);
        this.candyCount = 0;  // Reset candy count for next level
        localStorage.setItem('candyCount', this.candyCount);
        this.candiesForNextLevel = this.calculateRequiredCandies();
        
        // Show level up message
        const levelUpMessage = document.createElement('div');
        levelUpMessage.className = 'level-up-message';
        levelUpMessage.textContent = `Level Up! 🎉 Level ${this.level}`;
        document.body.appendChild(levelUpMessage);
        
        setTimeout(() => {
            levelUpMessage.remove();
        }, 2000);
    }

    updateDisplay() {
        this.multiplierElement.textContent = this.multiplier + 'x';
        this.candyCountElement.textContent = this.candyCount;
        this.levelElement.textContent = this.level;
        this.progressTextElement.textContent = `${this.candyCount}/${this.candiesForNextLevel}`;
        
        // Update progress bar
        const progress = (this.candyCount / this.candiesForNextLevel) * 100;
        this.progressFillElement.style.width = `${progress}%`;
    }
}

// Add CSS for level up message
const style = document.createElement('style');
style.textContent = `
    .level-up-message {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: rgba(76, 175, 80, 0.9);
        color: white;
        padding: 20px 40px;
        border-radius: 10px;
        font-size: 24px;
        animation: fadeInOut 2s ease-in-out;
        z-index: 1000;
    }

    @keyframes fadeInOut {
        0% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
        20% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
        30% { transform: translate(-50%, -50%) scale(1); }
        70% { opacity: 1; }
        100% { opacity: 0; }
    }
`;
document.head.appendChild(style);

// Start the game when the page loads
window.onload = () => {
    new ClickingGame();
};
