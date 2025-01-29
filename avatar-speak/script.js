document.addEventListener('DOMContentLoaded', () => {
    const textInput = document.getElementById('text-input');
    const speakBtn = document.getElementById('speak-btn');
    const speechText = document.getElementById('speech-text');
    const speechBubble = document.querySelector('.speech-bubble');
    const voiceSelect = document.getElementById('voice-select');
    const mouth = document.querySelector('.mouth-inner');

    // Initialize speech synthesis
    const synth = window.speechSynthesis;
    let voices = [];

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

        // Create utterance with "meow"
        const utterance = new SpeechSynthesisUtterance("meow");
        
        // Set selected voice
        if (voiceSelect.value !== '') {
            utterance.voice = voices[voiceSelect.value];
        }

        // Calculate pitch based on input text
        // Use text length to vary pitch between 0.5 and 2
        const basePitch = 1.0;
        const pitchVariation = (text.length % 10) / 5; // Will give value between 0 and 2
        utterance.pitch = Math.max(0.5, Math.min(2, basePitch + pitchVariation));

        // Adjust rate based on text characteristics
        const hasUpperCase = /[A-Z]/.test(text);
        utterance.rate = hasUpperCase ? 1.2 : 0.8;

        // Start speaking
        synth.speak(utterance);

        // Start mouth animation
        mouth.style.animation = 'speak 0.2s infinite alternate';

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
