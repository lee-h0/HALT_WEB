import { useMemo } from 'react'

interface TreeNode {
  name: string
  /** 파일이면 전체 경로, 폴더면 null */
  path: string | null
  children: TreeNode[]
}

/** 'src/cart.js' 같은 경로 목록을 폴더 구조로 바꾼다. 폴더가 먼저, 이름순 */
function buildTree(paths: string[]): TreeNode[] {
  const root: TreeNode = { name: '', path: null, children: [] }

  for (const fullPath of paths) {
    const parts = fullPath.split('/')
    let current = root
    parts.forEach((part, index) => {
      const isFile = index === parts.length - 1
      let child = current.children.find((node) => node.name === part && (node.path !== null) === isFile)
      if (!child) {
        child = { name: part, path: isFile ? fullPath : null, children: [] }
        current.children.push(child)
      }
      current = child
    })
  }

  const sort = (nodes: TreeNode[]): TreeNode[] =>
    nodes
      .map((node) => ({ ...node, children: sort(node.children) }))
      .sort((a, b) => {
        const aIsFolder = a.path === null
        const bIsFolder = b.path === null
        if (aIsFolder !== bIsFolder) return aIsFolder ? -1 : 1
        return a.name.localeCompare(b.name)
      })

  return sort(root.children)
}

interface FileTreeProps {
  paths: string[]
  activePath: string | null
  /** 수정한 파일 경로. 이름 옆에 점을 표시한다 */
  modifiedPaths: Set<string>
  onSelect: (path: string) => void
}

export default function FileTree({ paths, activePath, modifiedPaths, onSelect }: FileTreeProps) {
  const tree = useMemo(() => buildTree(paths), [paths])

  function renderNodes(nodes: TreeNode[], depth: number) {
    return nodes.map((node) => {
      const indent = { paddingLeft: `${12 + depth * 14}px` }

      if (node.path === null) {
        return (
          <div key={`dir:${depth}:${node.name}`}>
            <div className="tree-item tree-folder" style={indent}>
              ▾ {node.name}
            </div>
            {renderNodes(node.children, depth + 1)}
          </div>
        )
      }

      const path = node.path
      return (
        <button
          key={path}
          type="button"
          className={`tree-item tree-file${path === activePath ? ' active' : ''}`}
          style={indent}
          onClick={() => onSelect(path)}
        >
          {node.name}
          {modifiedPaths.has(path) && <span className="modified-dot" title="수정됨" />}
        </button>
      )
    })
  }

  return <div className="file-tree">{renderNodes(tree, 0)}</div>
}
