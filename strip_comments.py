"""
Strip Chinese comments from Java files.
- Remove single-line comments (//) that contain Chinese characters
- Remove block comments (/* */) that contain Chinese characters
- Preserve non-Chinese comments intact
- Preserve string literals that happen to contain Chinese
"""
import os
import re

CHINESE_RE = re.compile(r'[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]')

def has_chinese(text):
    return bool(CHINESE_RE.search(text))

def strip_chinese_comments(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    result = []
    in_block_comment = False
    block_comment_lines = []
    modified = False

    for line in lines:
        if in_block_comment:
            block_comment_lines.append(line)
            if '*/' in line:
                in_block_comment = False
                block_text = '\n'.join(block_comment_lines)
                if has_chinese(block_text):
                    modified = True
                    after_close = line.split('*/', 1)
                    if len(after_close) > 1 and after_close[1].strip():
                        result.append(after_close[1])
                else:
                    result.extend(block_comment_lines)
                block_comment_lines = []
            continue

        stripped = line.lstrip()

        if '/*' in line:
            idx = line.find('/*')
            before = line[:idx]
            if before.count('"') % 2 == 0:
                if '*/' in line[idx+2:]:
                    comment_end = line.find('*/', idx+2)
                    comment_text = line[idx:comment_end+2]
                    if has_chinese(comment_text):
                        new_line = line[:idx].rstrip() + line[comment_end+2:]
                        if new_line.strip():
                            result.append(new_line)
                        modified = True
                        continue
                    else:
                        result.append(line)
                        continue
                else:
                    in_block_comment = True
                    block_comment_lines = [line]
                    continue

        if '//' in line:
            idx = line.find('//')
            before = line[:idx]
            if before.count('"') % 2 == 0:
                comment_part = line[idx:]
                if has_chinese(comment_part):
                    code_part = before.rstrip()
                    if code_part:
                        result.append(code_part)
                    modified = True
                    continue

        result.append(line)

    if modified:
        cleaned = []
        blank_count = 0
        for line in result:
            if line.strip() == '':
                blank_count += 1
                if blank_count <= 2:
                    cleaned.append(line)
            else:
                blank_count = 0
                cleaned.append(line)

        new_content = '\n'.join(cleaned)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def main():
    root = r'e:\JMixin\Ice_and_Fire_RLCraft-1.20.1\src\main\java'
    count = 0
    for dirpath, dirnames, filenames in os.walk(root):
        for fname in filenames:
            if fname.endswith('.java'):
                fpath = os.path.join(dirpath, fname)
                if strip_chinese_comments(fpath):
                    rel = os.path.relpath(fpath, root)
                    print(f'Stripped: {rel}')
                    count += 1
    print(f'\nTotal files modified: {count}')

if __name__ == '__main__':
    main()
