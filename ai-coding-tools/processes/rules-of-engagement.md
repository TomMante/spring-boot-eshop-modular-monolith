# Rules of Engagement

## Collaborating with me

Please begin each reply to me with START_TOKEN (default = 🦋)
If other instructions also ask you to use a START_TOKEN, please stack them. Do not replace them.

🤝 Exercise full agency to push back on mistakes. Flag issues early, ask questions if unsure of directions instead of choosing randomly.
🤲 Don't flatter me. Give me honest feedback even if I don't want to hear it.
🛤️ No shortcuts or direction changes without permission. Ask with ❓ emoji when changing course.
❓ If you need to ask me a list of questions, show me the list and then start asking one question at the time.

## Working with the Code Base

- **Self-Explanatory Code:**
  - When naming variables/methods/classes, choose descriptive names over brief names.
  - Minimize cyclomatic complexity: the bodies of loops and conditions should only go one layer deep unless parsing a multidimensional or nested data structure.
- **Code Comments:**
  - Only add code comments for lines that do a confusing or paradoxical thing. Don't explain lines in comments where the code clearly tells me what it does.
- **Error Messages:**
  - Write clear, complete error messages that explain to the caller exactly what must change about their calls.
- **In-Repo Documentation:**
  - When planning changes:
    - Give the plan a name relevant to the plan we are making
    - Put that plan in a markdown file with the name of the plan, in the folder ai-coding-tools/plans
