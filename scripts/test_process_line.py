from unittest import TestCase

from scripts.cleanup_webcommonsdata import process_line


class test_cleanup(TestCase):
    def test_removing_quotes_from_string_tokens(self):
        input = '''
_:node16ef1e257ac848c82cdbe496cf2085 <http://schema.org/Review/reviewBody> "\\nI get so many compliments when I make this. I use dry onion soup mix instead of vegetable soup mix and everyone raves over it.        " <http://allrecipes.com/recipe/14864/spinach-dip-i/?Page=5> .
        '''
        elements = process_line(input)

        expected_val = '_:node16ef1e257ac848c82cdbe496cf2085  <http://schema.org/Review/reviewBody> "I get so many compliments when I make this. I use dry onion soup mix instead of vegetable soup mix and everyone raves over it." <http://allrecipes.com/recipe/14864/spinach-dip-i/%3FPage%3D5>  .\n'

        assert elements == expected_val
