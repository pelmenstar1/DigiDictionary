<h1 align="center">Digi Dictionary</h1> <br/>

A small Android application which helps you to learn new words and expressions.
Whenever you find a new word or expression you can add it with its meaning and some notes (for example, a sentence using that word)

You also can:
- View all of your expressions.
- Add, edit, delete them.
- Search
- Revise them. By default, you only see random expressions without their meanings.
The idea is to tell their meanings, click on the item and if you answer correct, then you click "Correct", if wrong, click "Wrong".
Each record has a score.
If it is answered correct in the revision, then some score points are added to score, if wrong, some points are subtracted.
In the settings, you can specify how much points you want to add, when your answer is correct, and subtract, when it's wrong.
In the next revision, records with negative score will definitely appear and you are given a chance to answer them again 
in hope you will do that correct.   

There's also a widget which shows you records that were added during last 24 hours. 

## Import / Export

If you want to transfer all of your expressions to another device, you can use `Import / Export` in the settings.
Export your records, transfer the file to another device, import the records using `Import` button and choose the file.
During the import, there can be conflicts when the application already contains the records in the file.
Note that when a record in the application and one in the file are the same, it's not considered as conflict.
Two records are considered as conflicting only when they have same expression and different meaning or additional notes.
So, when there are conflicting records, you can choose what to do with them: `Accept old`, `Accept new` or `Merge`.

## Screenshots

<p align="center">
  <img src="./art/screen1.jpg" width="400px">
  <img src="./art/screen2.jpg" width="400px">
  <img src="./art/screen3.jpg" width="400px">
</p>

## License

```
MIT License

Copyright (c) 2022 Khmaruk Oleg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```