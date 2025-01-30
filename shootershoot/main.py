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
YELLOW = (255, 255, 0)  # Color for power-ups

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
        self.double_shot = False
        self.triple_shot = False
        self.power_up_timer = 0
        self.triple_shot_timer = 0
        self.power_up_duration = 180  # Duration in frames (3 seconds at 60 FPS)
        self.triple_shot_duration = 180  # Duration in frames (3 seconds at 60 FPS)
        self.stars_collected = 0
        self.bonus_stars = 0
        self.permanent_power = False

    def update(self):
        self.rect.x += self.speed_x
        if self.rect.right > SCREEN_WIDTH:
            self.rect.right = SCREEN_WIDTH
        if self.rect.left < 0:
            self.rect.left = 0
            
        # Update power-up timer only if not permanent
        if self.double_shot and not self.permanent_power:
            self.power_up_timer += 1
            if self.power_up_timer >= self.power_up_duration:
                self.double_shot = False
                self.power_up_timer = 0

        # Update triple shot timer
        if self.triple_shot:
            self.triple_shot_timer += 1
            if self.triple_shot_timer >= self.triple_shot_duration:
                self.triple_shot = False
                self.triple_shot_timer = 0

    def collect_star(self):
        self.stars_collected += 1
        self.bonus_stars += 1
        
        # Check for permanent double shots
        if self.stars_collected >= 5:
            self.permanent_power = True
            self.double_shot = True
        else:
            # Temporary double shot for 3 seconds
            self.double_shot = True
            self.power_up_timer = 0
            
        # Check for temporary triple shots
        if self.bonus_stars >= 10:
            self.triple_shot = True
            self.triple_shot_timer = 0
            self.bonus_stars = 0  # Reset bonus stars after triple shot

    def shoot(self):
        bullets = []
        if self.triple_shot:
            # Create three bullets
            bullets.append(Bullet(self.rect.centerx + 5, self.rect.top))
            bullets.append(Bullet(self.rect.centerx + 15, self.rect.top))
            bullets.append(Bullet(self.rect.centerx + 25, self.rect.top))
        elif self.double_shot:
            # Create two bullets side by side
            bullets.append(Bullet(self.rect.centerx + 10, self.rect.top))
            bullets.append(Bullet(self.rect.centerx + 20, self.rect.top))
        else:
            # Create single bullet
            bullets.append(Bullet(self.rect.centerx + 15, self.rect.top))
        return bullets

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

class PowerUp(pygame.sprite.Sprite):
    def __init__(self, x, y):
        super().__init__()
        self.image = pygame.Surface((15, 15), pygame.SRCALPHA)
        # Draw a star shape for power-up
        pygame.draw.polygon(self.image, YELLOW, [
            (7, 0), (9, 5), (14, 5),
            (10, 8), (11, 13), (7, 10),
            (3, 13), (4, 8), (0, 5),
            (5, 5)
        ])
        self.rect = self.image.get_rect()
        self.rect.x = x
        self.rect.y = y
        self.speed_y = 2

    def update(self):
        self.rect.y += self.speed_y
        if self.rect.top > SCREEN_HEIGHT:
            self.kill()

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
power_ups = pygame.sprite.Group()  # New group for power-ups

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
font = pygame.font.Font(None, 36)  # Initialize font for star counter

while running:
    # Event handling
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False
        elif event.type == pygame.KEYDOWN:
            if event.key == pygame.K_SPACE:
                # Use the new shoot method
                new_bullets = player.shoot()
                for bullet in new_bullets:
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
        # 20% chance to spawn a power-up when enemy is destroyed
        if random.random() < 0.2:
            power_up = PowerUp(hit.rect.centerx, hit.rect.centery)
            all_sprites.add(power_up)
            power_ups.add(power_up)
        enemy = Enemy()
        all_sprites.add(enemy)
        enemies.add(enemy)

    # Check for player-power_up collisions
    power_up_hits = pygame.sprite.spritecollide(player, power_ups, True)
    if power_up_hits:
        player.collect_star()

    # Check for player-enemy collisions
    hits = pygame.sprite.spritecollide(player, enemies, False)
    if hits:
        running = False

    # Draw
    screen.fill(BLACK)
    
    # Draw stars background
    for star in stars:
        pygame.draw.circle(screen, WHITE, (star.x, star.y), star.size)
    
    all_sprites.draw(screen)

    # Draw star counters
    star_text = f"Double Shot Stars: {min(player.stars_collected, 5)}/5"
    if player.permanent_power:
        star_text += " (PERMANENT!)"
    elif player.double_shot:
        star_text += f" (ACTIVE: {(player.power_up_duration - player.power_up_timer) // 60}s)"
    text_surface = font.render(star_text, True, YELLOW)
    screen.blit(text_surface, (10, 10))

    # Draw bonus star counter for triple shot
    bonus_text = f"Triple Shot Stars: {player.bonus_stars}/10"
    if player.triple_shot:
        bonus_text += f" (ACTIVE: {(player.triple_shot_duration - player.triple_shot_timer) // 60}s)"
    bonus_surface = font.render(bonus_text, True, YELLOW)
    screen.blit(bonus_surface, (10, 40))

    pygame.display.flip()

    # Cap the frame rate
    clock.tick(60)

pygame.quit()
sys.exit()
