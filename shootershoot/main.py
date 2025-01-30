import pygame
import sys
import random
import math

# Initialize Pygame
pygame.init()

# Constants
SCREEN_WIDTH = 800
SCREEN_HEIGHT = 600
PLAYER_SPEED = 5
BULLET_SPEED = 7
ENEMY_SPEED = 3

# Colors
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
RED = (255, 0, 0)
BLUE = (30, 144, 255)
GRAY = (128, 128, 128)
DARK_BLUE = (0, 0, 139)
NAVY = (0, 0, 128)

# Set up the display
screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
pygame.display.set_caption("Battleship Shooter")
clock = pygame.time.Clock()

class Player(pygame.sprite.Sprite):
    def __init__(self):
        super().__init__()
        # Create a larger surface for the battleship
        self.image = pygame.Surface((50, 40), pygame.SRCALPHA)
        
        # Draw the battleship
        # Main body
        pygame.draw.rect(self.image, GRAY, (10, 15, 30, 20))
        # Front point
        pygame.draw.polygon(self.image, GRAY, [(40, 25), (50, 20), (40, 15)])
        # Bridge/tower
        pygame.draw.rect(self.image, DARK_BLUE, (20, 5, 15, 10))
        # Gun
        pygame.draw.rect(self.image, NAVY, (35, 18, 10, 4))
        
        self.rect = self.image.get_rect()
        self.rect.centerx = SCREEN_WIDTH // 2
        self.rect.bottom = SCREEN_HEIGHT - 10
        self.speed_x = 0

    def update(self):
        self.rect.x += self.speed_x
        if self.rect.right > SCREEN_WIDTH:
            self.rect.right = SCREEN_WIDTH
        if self.rect.left < 0:
            self.rect.left = 0

class Bullet(pygame.sprite.Sprite):
    def __init__(self, x, y):
        super().__init__()
        self.image = pygame.Surface((4, 10))
        self.image.fill(BLUE)
        self.rect = self.image.get_rect()
        self.rect.centerx = x
        self.rect.bottom = y
        self.speed_y = -BULLET_SPEED

    def update(self):
        self.rect.y += self.speed_y
        if self.rect.bottom < 0:
            self.kill()

class Enemy(pygame.sprite.Sprite):
    def __init__(self):
        super().__init__()
        # Create enemy ship surface
        self.image = pygame.Surface((40, 30), pygame.SRCALPHA)
        
        # Draw enemy ship (simpler design, red colored)
        pygame.draw.rect(self.image, RED, (5, 10, 30, 15))  # Main body
        pygame.draw.polygon(self.image, RED, [(35, 17), (40, 17), (35, 10)])  # Front
        
        self.rect = self.image.get_rect()
        self.rect.x = random.randrange(SCREEN_WIDTH - self.rect.width)
        self.rect.y = random.randrange(-100, -40)
        self.speed_y = ENEMY_SPEED

    def update(self):
        self.rect.y += self.speed_y
        if self.rect.top > SCREEN_HEIGHT:
            self.rect.x = random.randrange(SCREEN_WIDTH - self.rect.width)
            self.rect.y = random.randrange(-100, -40)

# Background stars
class Star:
    def __init__(self):
        self.x = random.randrange(SCREEN_WIDTH)
        self.y = random.randrange(SCREEN_HEIGHT)
        self.speed = random.randrange(1, 3)
        self.size = random.randrange(1, 3)

    def move(self):
        self.y += self.speed
        if self.y > SCREEN_HEIGHT:
            self.y = 0
            self.x = random.randrange(SCREEN_WIDTH)

# Create stars
stars = [Star() for _ in range(50)]

# Sprite groups
all_sprites = pygame.sprite.Group()
bullets = pygame.sprite.Group()
enemies = pygame.sprite.Group()

# Create player
player = Player()
all_sprites.add(player)

# Create initial enemies
for i in range(8):
    enemy = Enemy()
    all_sprites.add(enemy)
    enemies.add(enemy)

# Game loop
running = True
while running:
    # Event handling
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False
        elif event.type == pygame.KEYDOWN:
            if event.key == pygame.K_SPACE:
                bullet = Bullet(player.rect.centerx + 15, player.rect.top)  # Adjust bullet position
                all_sprites.add(bullet)
                bullets.add(bullet)

    # Get pressed keys
    keys = pygame.key.get_pressed()
    player.speed_x = (keys[pygame.K_RIGHT] - keys[pygame.K_LEFT]) * PLAYER_SPEED

    # Update
    all_sprites.update()

    # Move stars
    for star in stars:
        star.move()

    # Check for bullet-enemy collisions
    hits = pygame.sprite.groupcollide(enemies, bullets, True, True)
    for hit in hits:
        enemy = Enemy()
        all_sprites.add(enemy)
        enemies.add(enemy)

    # Check for player-enemy collisions
    hits = pygame.sprite.spritecollide(player, enemies, False)
    if hits:
        running = False

    # Draw
    screen.fill(BLACK)
    
    # Draw stars
    for star in stars:
        pygame.draw.circle(screen, WHITE, (star.x, star.y), star.size)
    
    all_sprites.draw(screen)
    pygame.display.flip()

    # Cap the frame rate
    clock.tick(60)

pygame.quit()
sys.exit()
