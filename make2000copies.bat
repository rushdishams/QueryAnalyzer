@echo off
cls
for /l %%# in (1,1,2000) do (
copy "H:\eclipse-luna\newworkspace\LuceneIndexing\filesToIndex\01-mod.txt" "H:\eclipse-luna\newworkspace\LuceneIndexing\filesToIndex\01-mod-%%#.txt" > nul
)