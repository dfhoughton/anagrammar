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

![female_names](https://cloud.githubusercontent.com/assets/177421/20863030/e0f650a6-b98a-11e6-84ef-6c2291dea52f.png)

compile the necessary wordlists into tries, and then generate from a particular phrase
all the anagrams of that phrase that fit within the wordlists and grammar.

## Usage

If you compile the command line utility, it will give you usage information in the usual way.
Note that you need my [CLI](https://github.com/dfhoughton/cli) library to compile the code. 

    ~ $ java -jar anagrammar.jar --help
    USAGE: anagrammar [options] <word>*

      compute the anagrams of a phrase that obey a specified grammar

        --grammar -g   <str>   specify a grammar to use rather than the default
                               listed in /Users/houghton/.anagrammar/config
        --out -o       <file>  dump output -- anagrams, grammar, or Graphviz spec
                               -- into this file
        --sample -s    <int>   produce only a sample of anagrams; value must be > 0
        --random -r            generate anagrams in random order
        --unique -u            in case the grammar can produce the same phrase in
                               more than one way, this ensures that each name is only
                               listed once; NOTE: for the sake of memory efficiency 
                               and speed, hashcodes are used to determine uniqueness,
                               so some anagrams may be dropped altogether
        --count -c             print out the number of anagrams found

        --list -l              list available grammars
        --word-lists           show the list of word lists used by the grammars
        --dot                  print out a Graphviz graph specification for the
                               finite state automaton representation of a grammar
        --show-grammar         dump out the selected grammar

        --threads      <int>   maximum number of threads; value must be > 0;
                               default: 8

        --initialize           generate a skeleton configuration file in
                               /Users/houghton/.anagrammar; you must then modify this
                               configuration file to specify grammars and word lists
        --force                in conjunction with --initialize, this overwrites an
                               existing configuration file

        --version -v           print anagrammar version
        --help -? -h           print usage information

    This utility allows you to generate all the anagrams of a phrase that obey a 
    particular grammar. In order to do this it needs a grammar and word lists that 
    define the acceptable values of terminal nodes in the grammar. For instance, you
    might have the grammar

      name -> <first>
      name -> <last>
      name -> <first> <last>

    This grammar says a name can be a "first" by itself, a "last" by itself, or a 
    "first" followed by a "last". Just what these things are must be specified by two
    word lists: first and last. To use this grammar, you must configure this 
    application to find the grammar and the word lists. Run the --initialize command
    if you have not already and then edit the configuration file it generates to tell
    anagrammar where to find the grammar file and the word lists it requires. A 
    specification of the grammar formalism can be found in the README file in the 
    same directory as the configuration file.

The aforementioned README is [here](src/README).