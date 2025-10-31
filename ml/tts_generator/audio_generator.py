import torch
import numpy as np
import scipy

from TeraTTS import TTS

from transformers import VitsModel, AutoTokenizer

from ruaccent import RUAccent
accentizer = RUAccent()

tts = TTS("TeraTTS/natasha-g2p-vits", add_time_to_end=1.0)
accentizer.load(omograph_model_size='turbo', use_dictionary=True)

class TTSGenerator():
    def __init__(self, text):
        self.text = text
    
    def create_file(self, speed=1.2):
        input = accentizer.load(self.text)
        self.audio = tts(input, lenght_scale=speed)

    def save_file(self, filepath, filename):
        tts.save_wav(self.audio, f"{filepath}/{filename}.wav")