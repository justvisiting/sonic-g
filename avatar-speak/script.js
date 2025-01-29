document.addEventListener('DOMContentLoaded', () => {
    const textInput = document.getElementById('text-input');
    const speakBtn = document.getElementById('speak-btn');
    const speechText = document.getElementById('speech-text');
    const speechBubble = document.querySelector('.speech-bubble');
    const voiceSelect = document.getElementById('voice-select');
    const avatarSelect = document.getElementById('avatar-select');
    const mouth = document.querySelector('.mouth-inner');

    // Initialize speech synthesis
    const synth = window.speechSynthesis;
    let voices = [];

    // Avatar configurations
    const avatarConfigs = {
        'default': {
            processText: (text) => text,
            voiceSettings: () => ({
                rate: 0.9,  // Slightly slower for rhythm
                pitch: 1.0
            })
        },
        'cool-cat': {
            processText: (text) => {
                // Convert each word to meow
                const words = text.split(/\s+/);
                return words.map(() => 'meow').join(' ');
            },
            voiceSettings: (text) => ({
                pitch: 1.0 + (text.length % 5) * 0.2,  // Vary pitch based on text length
                rate: 0.8  // Slower for more distinct meows
            })
        },
        'robot': {
            processText: (text) => text,
            voiceSettings: () => ({
                pitch: 0.7,
                rate: 0.7  // Slower for robotic rhythm
            })
        },
        'alien': {
            processText: (text) => text,
            voiceSettings: () => ({
                pitch: 1.3,
                rate: 1.1  // Slightly faster but still rhythmic
            })
        }
    };

    // Populate voice options
    function populateVoices() {
        voices = synth.getVoices();
        voiceSelect.innerHTML = '';
        
        voices.forEach((voice, index) => {
            const option = document.createElement('option');
            option.value = index;
            option.textContent = `${voice.name} (${voice.lang})`;
            voiceSelect.appendChild(option);
        });
    }

    // Handle voices changed event
    synth.addEventListener('voiceschanged', populateVoices);

    // Initial population of voices
    populateVoices();

    // Function to make the avatar speak
    function speak(text) {
        // Show speech bubble with text
        speechText.textContent = text;
        speechBubble.classList.remove('hidden');

        const selectedAvatar = avatarSelect.value;
        const avatarConfig = avatarConfigs[selectedAvatar];
        
        // Process text based on avatar type
        const processedText = avatarConfig.processText(text);
        
        // Clear any existing speech
        synth.cancel();

        // Split text into words and add rhythmic pauses
        const words = processedText.split(/\s+/);
        let utteranceText = words.join(' . '); // Add pauses between words
        
        const utterance = new SpeechSynthesisUtterance(utteranceText);
        
        // Set selected voice
        if (voiceSelect.value !== '') {
            utterance.voice = voices[voiceSelect.value];
        }

        // Get voice settings
        const settings = avatarConfig.voiceSettings(text);

        // Apply voice settings and add rhythm
        Object.assign(utterance, {
            ...settings,
            rate: settings.rate || 1, // Use configured rate or default
            pitch: settings.pitch || 1, // Use configured pitch or default
        });

        // Start speaking
        synth.speak(utterance);

        // Start mouth animation with rhythm
        mouth.style.animation = 'speak 0.3s infinite alternate';

        // Handle speech end
        utterance.onend = () => {
            // Stop mouth animation
            mouth.style.animation = '';
            
            // Hide speech bubble after a delay
            setTimeout(() => {
                speechBubble.classList.add('hidden');
            }, 1000);
        };
    }

    // Handle speak button click
    speakBtn.addEventListener('click', () => {
        const text = textInput.value.trim();
        if (text) {
            speak(text);
        }
    });

    // Handle enter key press
    textInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const text = textInput.value.trim();
            if (text) {
                speak(text);
            }
        }
    });

    // Add some eye movement
    const eyes = document.querySelectorAll('.eye');
    document.addEventListener('mousemove', (e) => {
        eyes.forEach(eye => {
            const rect = eye.getBoundingClientRect();
            const x = (rect.left + rect.width / 2);
            const y = (rect.top + rect.height / 2);
            const rad = Math.atan2(e.pageX - x, e.pageY - y);
            const rot = (rad * (180 / Math.PI) * -1) + 180;
            eye.style.transform = `rotate(${rot}deg)`;
        });
    });
});
