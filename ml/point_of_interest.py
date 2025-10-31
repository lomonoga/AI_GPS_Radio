import os
import numpy as np

from scipy.spatial import distance

from constants import CATEGORIES
from tts_generator import TTSGenerator
from llm_generator import LLMGenerator

class POI:
    def __init__(self, x_pos, y_pos, description, categories, facts_description=None):
        self.id = 1
        self.x_pos = x_pos
        self.y_pos = y_pos

        self.categories = [CATEGORIES[x] for x in sorted(categories)]

        self.short_description = description
        self.facts_description = facts_description

    def __generate_audio(self, text, filename):
        audio_gen = TTSGenerator(text)
        audio_gen.create_file()
        audio_gen.save_file(filepath=f'poi_files/audio_files/id_{self.id}', filename=filename)
        return f'poi_files/audio_files/id_{self.id}/{filename}.wav'

    def __generate_llm_desc(self):
        llm_gen = LLMGenerator(self.short_description)
        return llm_gen.create_desc()

    def get_audio(self, type='short_desc'):
        if type not in ['short_desc', 'facts_desc']:
            raise ValueError('type must be in [short_desc, facts_desc]')
        
        check_available_audio = {
            'short_desc': 'short_desc.wav' not in os.listdir(f'poi_files/audio_files/id_{self.id}'),
            'facts_desc': all(['fact_' not in x for x in os.listdir(f'poi_files/audio_files/id_{self.id}')]),
        }

        if self.facts_description is None:
            self.facts_description = self.__generate_llm_desc()

        if type == 'short_desc' and check_available_audio[type]:
            self.short_audio = [self.__generate_audio(self.short_description, 'short_desc')]
            return self.short_audio
        else:
            self.facts_audio = [self.__generate_audio(x, f'fact_{i}') for x in enumerate(self.facts_description)]
            return self.facts_audio
        

    def get_distance(self, x_pos, y_pos):
        return distance.euclidean([self.x_pos, self.y_pos], [x_pos, y_pos])
    
    def get_relevant_level(self, user_categories):
        user_categories = [CATEGORIES[x] for x in sorted(user_categories)]
        cosine_similarity = np.dot(self.categories, user_categories) / (np.linalg.norm(self.categories) * np.linalg.norm(user_categories))
        print(cosine_similarity)