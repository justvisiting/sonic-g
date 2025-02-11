class ClickingGame {
    constructor() {
        this.multiplier = 1;
        this.clickStreak = 0;
        this.lastClickTime = Date.now();
        this.candyCount = parseInt(localStorage.getItem('candyCount')) || 0;
        this.level = parseInt(localStorage.getItem('level')) || 1;
        this.candiesForNextLevel = this.calculateRequiredCandies();
        
        // Shop items owned and active status
        this.ownedItems = JSON.parse(localStorage.getItem('ownedItems')) || {
            autoClicker: false,
            speedBoost: false,
            goldenClick: false
        };
        
        // Active status for owned items
        this.activeItems = JSON.parse(localStorage.getItem('activeItems')) || {
            autoClicker: false,
            speedBoost: false,
            goldenClick: false
        };

        // DOM elements
        this.target = document.getElementById('target');
        this.multiplierElement = document.getElementById('multiplier');
        this.candyCountElement = document.getElementById('candyCount');
        this.levelElement = document.getElementById('level');
        this.progressTextElement = document.getElementById('progressText');
        this.progressFillElement = document.getElementById('progressFill');
        
        // Shop elements
        this.shopButton = document.getElementById('shopButton');
        this.shopPanel = document.getElementById('shopPanel');
        this.closeShopButton = document.getElementById('closeShop');
        
        // Initialize
        this.updateDisplay();
        this.setupEventListeners();
        this.setupShop();
        
        // Start auto clicker if owned and active
        if (this.ownedItems.autoClicker && this.activeItems.autoClicker) {
            this.startAutoClicker();
        }
    }

    setupShop() {
        // Shop toggle
        this.shopButton.addEventListener('click', () => {
            this.shopPanel.classList.add('open');
            this.updateShopItems();
        });
        
        this.closeShopButton.addEventListener('click', () => {
            this.shopPanel.classList.remove('open');
        });

        // Buy/toggle buttons
        document.querySelectorAll('.buy-button').forEach(button => {
            button.addEventListener('click', () => {
                const itemId = button.dataset.item;
                if (this.ownedItems[itemId]) {
                    this.toggleItem(itemId);
                } else {
                    this.buyItem(itemId);
                }
            });
        });

        this.updateShopItems();
    }

    updateShopItems() {
        document.querySelectorAll('.shop-item').forEach(item => {
            const requiredLevel = parseInt(item.dataset.level);
            const price = parseInt(item.dataset.price);
            const itemId = item.querySelector('.buy-button').dataset.item;
            const buyButton = item.querySelector('.buy-button');

            // Check if already owned
            if (this.ownedItems[itemId]) {
                buyButton.textContent = this.activeItems[itemId] ? 'Disable' : 'Enable';
                item.classList.remove('locked');
                buyButton.disabled = false;
                return;
            }

            // Check level requirement and price
            if (this.level < requiredLevel || this.candyCount < price) {
                item.classList.add('locked');
                buyButton.disabled = true;
            } else {
                item.classList.remove('locked');
                buyButton.disabled = false;
            }
            buyButton.textContent = 'Buy';
        });
    }

    toggleItem(itemId) {
        this.activeItems[itemId] = !this.activeItems[itemId];
        localStorage.setItem('activeItems', JSON.stringify(this.activeItems));
        
        // Handle special cases when toggling
        if (itemId === 'autoClicker') {
            if (this.activeItems.autoClicker) {
                this.startAutoClicker();
            }
        }
        
        this.updateShopItems();
    }

    buyItem(itemId) {
        const item = document.querySelector(`.shop-item[data-price][data-level] .buy-button[data-item="${itemId}"]`)
            .closest('.shop-item');
        const price = parseInt(item.dataset.price);
        
        if (this.candyCount >= price) {
            this.candyCount -= price;
            this.ownedItems[itemId] = true;
            this.activeItems[itemId] = true; // Activate immediately when bought
            localStorage.setItem('ownedItems', JSON.stringify(this.ownedItems));
            localStorage.setItem('activeItems', JSON.stringify(this.activeItems));
            localStorage.setItem('candyCount', this.candyCount);
            
            // Apply item effects
            if (itemId === 'autoClicker') {
                this.startAutoClicker();
            }
            
            this.updateDisplay();
            this.updateShopItems();
        }
    }

    startAutoClicker() {
        // Use requestAnimationFrame for smooth continuous clicking
        const autoClick = () => {
            if (this.ownedItems.autoClicker && this.activeItems.autoClicker) {
                // Auto clicker gives candies based on level
                const candiesEarned = this.level;
                this.candyCount += candiesEarned;
                localStorage.setItem('candyCount', this.candyCount);
                
                // Only show visual effect occasionally to prevent overwhelming
                if (Math.random() < 0.1) {  // 10% chance to show effect
                    this.showCandyEarnedEffect(candiesEarned);
                }
                
                this.updateDisplay();
                
                if (this.candyCount >= this.candiesForNextLevel) {
                    this.levelUp();
                }
            }
            requestAnimationFrame(autoClick);
        };
        
        // Start the continuous auto clicking
        requestAnimationFrame(autoClick);
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
        
        // Base candies is equal to current level
        let candiesEarned = this.level * this.multiplier;
        
        // Apply speed boost if owned and active
        if (this.ownedItems.speedBoost && this.activeItems.speedBoost && timeDiff < 500) {
            candiesEarned *= 2;
        }
        
        // Apply golden click if owned and active
        if (this.ownedItems.goldenClick && this.activeItems.goldenClick && Math.random() < 0.05) {
            candiesEarned *= 5;
            this.showGoldenClickEffect();
        }

        this.candyCount += candiesEarned;
        localStorage.setItem('candyCount', this.candyCount);

        // Show candy earned effect
        this.showCandyEarnedEffect(candiesEarned);

        // Check for level up
        if (this.candyCount >= this.candiesForNextLevel) {
            this.levelUp();
        }

        this.lastClickTime = currentTime;
        this.updateDisplay();
    }

    showGoldenClickEffect() {
        const effect = document.createElement('div');
        effect.className = 'golden-click-effect';
        effect.textContent = '✨ 5x Bonus! ✨';
        document.body.appendChild(effect);
        
        setTimeout(() => {
            effect.remove();
        }, 1000);
    }

    showCandyEarnedEffect(amount) {
        const effect = document.createElement('div');
        effect.className = 'candy-earned-effect';
        effect.textContent = `+${amount} 🍬`;
        
        // Position near the target
        const target = this.target.getBoundingClientRect();
        effect.style.left = `${target.left + target.width / 2}px`;
        effect.style.top = `${target.top - 30}px`;
        
        document.body.appendChild(effect);
        
        setTimeout(() => {
            effect.remove();
        }, 1000);
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

        // Update shop items after level up
        this.updateShopItems();
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

// Add CSS for effects
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

    .golden-click-effect {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        color: #FFD700;
        font-size: 24px;
        text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
        animation: fadeInOut 1s ease-in-out;
        z-index: 1000;
    }

    .candy-earned-effect {
        position: absolute;
        color: #FFD700;
        font-size: 18px;
        text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
        animation: fadeInOut 1s ease-in-out;
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
