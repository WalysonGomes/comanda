const [major, minor] = process.versions.node.split('.').map(Number)

if (major !== 22 || minor < 22) {
  throw new Error(`Comanda frontend requires Node 22.22.x (current: ${process.versions.node}). Run \`nvm use\` or use the Maven lifecycle.`)
}
