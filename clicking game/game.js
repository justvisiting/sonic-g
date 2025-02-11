class ClickingGame {
    constructor() {
        this.multiplier = 1;
        this.clickStreak = 0;
        this.lastClickTime = Date.now();
        this.candyCount = parseInt(localStorage.getItem('candyCount')) || 0;

        // DOM elements
        this.target = document.getElementById('target');
        this.multiplierElement = document.getElementById('multiplier');
        this.candyCountElement = document.getElementById('candyCount');

        // Initialize
        this.updateDisplay();
        this.setupEventListeners();
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

        this.lastClickTime = currentTime;
        this.updateDisplay();
        this.animateTarget();
    }

    updateDisplay() {
        this.multiplierElement.textContent = this.multiplier + 'x';
        this.candyCountElement.textContent = this.candyCount;
    }

    animateTarget() {
        // Random position within visible area
        const maxX = window.innerWidth - 200;
        const maxY = window.innerHeight - 200;
        const randomX = Math.max(50, Math.random() * maxX);
        const randomY = Math.max(50, Math.random() * maxY);

        this.target.style.transform = 'translate(' + 
            (randomX - this.target.offsetLeft) + 'px, ' + 
            (randomY - this.target.offsetTop) + 'px)';
    }
}

// Start the game when the page loads
window.onload = () => {
    new ClickingGame();
};
