# anagrammar
generate anagrams that obey a grammar

Anagrammar provides libraries, and a command line utility, that let you compile grammars like
this:

   female_name        -> female_first_part last_part?
   female_name        -> last_part
   female_first_part  -> <female_title> female_post_title?
   female_first_part  -> female_post_title
   female_post_title  -> <female> female_middle_part?
   female_post_title  -> female_middle_part
   female_middle_part -> <initial>{1,3}
   female_middle_part -> <female>{1,2} <surname>{1,2}
   female_middle_part -> <female>{1,2}
   female_middle_part -> <surname>{1,2}
   
   last_part -> <epithet>? <surname> <suffix>?

into a finite state automaton like this:

![Image of FSA]
(https://github.com/dfhoughton/anagrammar/female_names.svg)

compile the necessary wordlists into tries, and then generate from a particular phrase
all the anagrams of that phrase that fit within the wordlists and grammar.
